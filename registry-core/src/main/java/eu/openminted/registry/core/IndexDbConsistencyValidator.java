package eu.openminted.registry.core;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IndexDbConsistencyValidator {

    private static final Logger logger = LoggerFactory.getLogger(IndexDbConsistencyValidator.class);

    private final RestHighLevelClient client;
    private final ElasticOperationsService elasticOperationsService;
    private final ResourceTypeService resourceTypeService;
    private final ResourceService resourceService;
    private final DataSource dataSource;

    public IndexDbConsistencyValidator(RestHighLevelClient client,
                                       ElasticOperationsService elasticOperationsService,
                                       ResourceTypeService resourceTypeService,
                                       ResourceService resourceService,
                                       DataSource dataSource) {
        this.client = client;
        this.elasticOperationsService = elasticOperationsService;
        this.resourceTypeService = resourceTypeService;
        this.resourceService = resourceService;
        this.dataSource = dataSource;
    }

    @PostConstruct
    private void reindexOnInit() {
        ensureDatabaseIndexConsistency();
    }

    /**
     * <p>
     * Performs the following two operations to ensure data integrity.
     * </p>
     * <p>1. Reindexes all {@link Resource resources} from Database to Elastic.</p>
     * <p>2. Checks Database for missing resources and prints errors.</p>
     */
    public void ensureDatabaseIndexConsistency() {
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
                    .collect(Collectors.toList())
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
     */
    private List<String> findAllResourceIdsFromElasticIndex(String resourceType) throws IOException {
        List<String> resourceIds = new ArrayList<>();

        final Scroll scroll = new Scroll(TimeValue.timeValueSeconds(1L));
        SearchRequest searchRequest = new SearchRequest(resourceType);
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .size(10000)
                .docValueField("*_id")
                .fetchSource(false)
                .explain(true);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        while (searchHits != null && searchHits.length > 0) {
            resourceIds.addAll(
                    Arrays.stream(searchHits)
                            .map(hit -> (String) hit.getFields().get("_id").getValue())
                            .collect(Collectors.toList())
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
     * Returns all resource ids of an index.
     *
     * @param resourceType The {@link ResourceType} describing the index.
     * @return {@link List}
     */
    private List<String> fetchResourceIdsFromIndex(String resourceType) {
        List<String> resourceIds = new ArrayList<>();

        try {
            resourceIds = findAllResourceIdsFromElasticIndex(resourceType);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
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
        logger.info("Reindexing {} missing resources.", ids.size());
        for (String missingId : ids) {
            Resource resource = resourceService.getResource(missingId);
            logger.trace("Adding resource with id '{}' to index '{}'", resource.getId(), resource.getResourceTypeName());
            elasticOperationsService.add(resource);
        }
    }
}