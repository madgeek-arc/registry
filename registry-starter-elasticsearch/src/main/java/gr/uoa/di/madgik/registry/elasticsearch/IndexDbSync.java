/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.elasticsearch;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import jakarta.annotation.PostConstruct;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IndexDbSync {

    private static final Logger logger = LoggerFactory.getLogger(IndexDbSync.class);

    private final RestHighLevelClient client;
    private final IndexOperationsService indexOperationsService;
    private final ResourceTypeService resourceTypeService;
    private final ResourceService resourceService;
    private final DataSource dataSource;
    private final TaskExecutor taskExecutor = new VirtualThreadTaskExecutor();

    public IndexDbSync(RestHighLevelClient client,
                       IndexOperationsService indexOperationsService,
                       ResourceTypeService resourceTypeService,
                       ResourceService resourceService,
                       @Qualifier("registryDataSource") DataSource dataSource) {
        this.client = client;
        this.indexOperationsService = indexOperationsService;
        this.resourceTypeService = resourceTypeService;
        this.resourceService = resourceService;
        this.dataSource = dataSource;
    }

    @PostConstruct
    private void reindexOnInit() {
        taskExecutor.execute(new Runnable() {
            public void run() {
                ensureDatabaseIndexConsistency();
            }
        });
    }

    /**
     * <p>
     * Performs the following two operations to ensure data integrity.
     * </p>
     * <p>1. Reindexes all {@link Resource resources} from Database to Elastic.</p>
     * <p>2. Checks Database for missing resources and prints errors.</p>
     */
    public void ensureDatabaseIndexConsistency() {
        logger.info("Checking for index inconsistencies");
        resourceTypeService.getAllResourceType()
                .forEach(resourceType -> {
                    reindex(resourceType.getName());
                    checkDatabaseConsistency(resourceType.getName());
                });
    }

    /**
     * Reindex all Database {@link Resource resources} to Elastic.
     */
    public void reindex() {
        resourceTypeService.getAllResourceType()
                .forEach(resourceType -> reindex(resourceType.getName()));
    }

    /**
     * Fetches the ids of all {@link Resource resources} of a given {@link ResourceType resource type}
     * from the Database.
     *
     * @param resourceType The {@link ResourceType} to search.
     * @return {@link List}
     */
    private List<String> fetchResourceIdsFromDatabase(String resourceType) {
        List<String> databaseResources = new ArrayList<>();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();

        String query = "SELECT id FROM " + resourceType + "_view";

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);
        if (!records.isEmpty()) {
            databaseResources.addAll(records
                    .stream()
                    .map(r -> (String) r.get("id"))
                    .toList()
            );
        }
        return databaseResources;
    }

    /**
     * Returns all resource ids of the requested index ({@link ResourceType}) from Elastic.
     *
     * @param resourceType The {@link ResourceType} describing the index.
     * @return {@link List}
     * @throws IOException
     * @throws ElasticsearchStatusException - When index is missing
     */
    private List<String> findAllResourceIdsFromElasticIndex(String resourceType) throws IOException, ElasticsearchStatusException {
        List<String> resourceIds = new ArrayList<>();

        final Scroll scroll = new Scroll(TimeValue.timeValueSeconds(1L));
        SearchRequest searchRequest = new SearchRequest(resourceType);
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .size(10000)
                .fetchField("*_id")
                .fetchSource(false)
                .explain(false);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        while (searchHits != null && searchHits.length > 0) {
            resourceIds.addAll(
                    Arrays.stream(searchHits)
                            .map(SearchHit::getId)
                            .toList()
            );

            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            searchHits = searchResponse.getHits().getHits();
        }

        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        boolean succeeded = clearScrollResponse.isSucceeded();
        if (!succeeded) {
            logger.error("clear scroll request failed...");
        }

        return resourceIds;
    }

    /**
     * Returns all resource ids of an index. If index is missing, the method creates it.
     *
     * @param resourceType The {@link ResourceType} describing the index.
     * @return {@link List}
     */
    private List<String> fetchResourceIdsFromIndex(String resourceType) {
        List<String> resourceIds = new ArrayList<>();

        boolean done = false;
        short retries = 5;
        do {
            try {
                resourceIds = findAllResourceIdsFromElasticIndex(resourceType);
                done = true;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } catch (ElasticsearchStatusException e) {
                logger.warn(e.getMessage());
                // index must be missing - add it
                resourceTypeService.addResourceType(resourceTypeService.getResourceType(resourceType));
            }
            retries--;
        } while (!done && retries > 0);
        return resourceIds;
    }

    /**
     * Reindex missing resources of a {@link ResourceType}.
     *
     * @param resourceType The {@link ResourceType} describing the index to populate.
     */
    private void reindex(String resourceType) {
        List<String> indexResources = fetchResourceIdsFromIndex(resourceType);
        List<String> databaseResources = fetchResourceIdsFromDatabase(resourceType);

        List<String> missingIndexIds = new ArrayList<>(databaseResources);
        missingIndexIds.removeAll(indexResources);
        if (!missingIndexIds.isEmpty()) {
            logger.debug("Reindexing missing resources [{}] on {}", missingIndexIds, resourceType);
            reindexByIds(missingIndexIds);
        } else {
            logger.debug("Index is consistent with Database on {}", resourceType);
        }
    }

    /**
     * Checks whether every resource of an index ({@link ResourceType}) exists in the database.
     *
     * @param resourceType The {@link ResourceType} to perform the check on.
     */
    private void checkDatabaseConsistency(String resourceType) {
        List<String> indexResources = fetchResourceIdsFromIndex(resourceType);
        List<String> databaseResources = fetchResourceIdsFromDatabase(resourceType);

        List<String> missingDBIds = new ArrayList<>(indexResources);
        missingDBIds.removeAll(databaseResources);
        if (!missingDBIds.isEmpty()) {
            logger.error("Database is missing the following resources [{}] on {}", missingDBIds, resourceType);
        } else {
            logger.debug("Database is consistent with Index on {}", resourceType);
        }
    }

    /**
     * Reindex {@link Resource resources}.
     *
     * @param ids The resources' ids to reindex.
     */
    private void reindexByIds(List<String> ids) {
        logger.info("Reindexing {} missing resource{}.", ids.size(), ids.size() == 1 ? "" : "s");
        // TODO: Improve performance:
        //  1. create method returning multiple resources by id
        //  2. use indexOperationsService.addBulk() method to add them to the index
        for (String missingId : ids) {
            Resource resource = resourceService.getResource(missingId);
            logger.trace("Adding resource with id '{}' to index '{}'", resource.getId(), resource.getResourceTypeName());
            indexOperationsService.add(resource);
        }
        logger.info("Reindexing finished.");
    }
}