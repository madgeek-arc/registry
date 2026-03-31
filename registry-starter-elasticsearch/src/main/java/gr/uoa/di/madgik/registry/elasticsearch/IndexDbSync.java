/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import jakarta.annotation.PostConstruct;
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
import java.util.List;
import java.util.Map;

/**
 * Background consistency check between the SQL-backed registry and the Elasticsearch index.
 *
 * <p>At startup, this component enumerates each registered resource type, recreates missing
 * Elasticsearch indices on demand, re-indexes database rows that are absent from Elasticsearch,
 * and logs the inverse mismatch when Elasticsearch contains documents that no longer exist in
 * the database.</p>
 */
public class IndexDbSync {

    private static final Logger logger = LoggerFactory.getLogger(IndexDbSync.class);

    private final ElasticsearchClient elasticsearchClient;
    private final IndexOperationsService indexOperationsService;
    private final ResourceTypeService resourceTypeService;
    private final ResourceService resourceService;
    private final DataSource dataSource;
    private final TaskExecutor taskExecutor = new VirtualThreadTaskExecutor();

    public IndexDbSync(ElasticsearchClient elasticsearchClient,
                       IndexOperationsService indexOperationsService,
                       ResourceTypeService resourceTypeService,
                       ResourceService resourceService,
                       @Qualifier("registryDataSource") DataSource dataSource) {
        this.elasticsearchClient = elasticsearchClient;
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
     * Returns all document ids for the given Elasticsearch index using the scroll API.
     *
     * <p>The request fetches ids only, keeping the response payload small while walking large
     * indices. Transport and parsing failures are wrapped in {@link ServiceException}.</p>
     */
    private List<String> findAllResourceIdsFromElasticIndex(String resourceType) {
        List<String> resourceIds = new ArrayList<>();
        String scrollId = null;
        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index(resourceType)
                    .scroll(t -> t.time("1m"))
                    .size(10000)
                    .source(src -> src.fetch(false))
                    .query(q -> q.matchAll(m -> m)),
                    Void.class);
            scrollId = response.scrollId();
            List<Hit<Void>> hits = response.hits().hits();
            while (!hits.isEmpty()) {
                hits.stream().map(Hit::id).forEach(resourceIds::add);
                String currentScrollId = scrollId;
                ScrollResponse<Void> scrollResponse = elasticsearchClient.scroll(
                        sr -> sr.scrollId(currentScrollId).scroll(t -> t.time("1m")),
                        Void.class);
                scrollId = scrollResponse.scrollId();
                hits = scrollResponse.hits().hits();
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to read Elasticsearch index " + resourceType, e);
        } finally {
            clearScroll(scrollId);
        }
        return resourceIds;
    }

    /**
     * Best-effort cleanup of an active Elasticsearch scroll context.
     */
    private void clearScroll(String scrollId) {
        if (scrollId == null || scrollId.isBlank()) {
            return;
        }
        try {
            elasticsearchClient.clearScroll(cs -> cs.scrollId(scrollId));
        } catch (Exception e) {
            logger.error("clear scroll request failed", e);
        }
    }

    /**
     * Loads all ids from Elasticsearch, creating the index first when the backend reports 404.
     */
    private List<String> fetchResourceIdsFromIndex(String resourceType) {
        List<String> resourceIds = new ArrayList<>();

        if (!exists(resourceType)) {
            logger.warn("Elasticsearch index '{}' is missing. Recreating it from the resource type definition.", resourceType);
            resourceTypeService.addResourceType(resourceTypeService.getResourceType(resourceType));
            return resourceIds;
        }

        boolean done = false;
        short retries = 5;
        do {
            try {
                resourceIds = findAllResourceIdsFromElasticIndex(resourceType);
                done = true;
            } catch (ServiceException e) {
                if (isNotFound(e)) {
                    logger.warn("Elasticsearch index '{}' missing. Recreating it from the resource type definition.", resourceType);
                    resourceTypeService.addResourceType(resourceTypeService.getResourceType(resourceType));
                    return new ArrayList<>();
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
            retries--;
        } while (!done && retries > 0);
        return resourceIds;
    }

    /**
     * Checks whether an Elasticsearch index currently exists.
     */
    private boolean exists(String indexName) {
        try {
            return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
        } catch (IOException e) {
            throw new ServiceException("Failed to check index existence for " + indexName, e);
        }
    }

    /**
     * Unwraps nested exceptions to detect an Elasticsearch HTTP 404 response.
     */
    private boolean isNotFound(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ElasticsearchException esEx && esEx.status() == 404) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Reindexes database rows that are missing from the target Elasticsearch index.
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
