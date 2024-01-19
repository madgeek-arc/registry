package eu.openminted.registry.core;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
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
    private void checkUponInit() {
        performCheck();
    }

    public void performCheck() {
        List<String> resourceTypeNames = resourceTypeService.getAllResourceType()
                .stream()
                .map(ResourceType::getName)
                .collect(Collectors.toList());

        for (String resourceTypeName : resourceTypeNames) {
            enforceIndexConsistency(resourceTypeName);
            checkDBConsistency(resourceTypeName);
        }
    }

    private List<String> fetchResourceIdsFromDB(String resourceType) {
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

    private List<String> findAllResourceIdsFromIndex(String resourceType) throws IOException {
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

    private List<String> fetchResourceIdsFromIndex(String resourceType) {
        List<String> resourceIds = new ArrayList<>();

        try {
            resourceIds = findAllResourceIdsFromIndex(resourceType);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return resourceIds;
    }

    private void enforceIndexConsistency(String resourceType) {
        List<String> indexResources = fetchResourceIdsFromIndex(resourceType);
        List<String> databaseResources = fetchResourceIdsFromDB(resourceType);

        List<String> missingIndexIds = new ArrayList<>(databaseResources);
        missingIndexIds.removeAll(indexResources);
        if (!missingIndexIds.isEmpty()) {
            logger.debug("Reindexing missing resources [{}] on {}", missingIndexIds, resourceType);
            indexMissingIds(missingIndexIds, resourceType);
        } else {
            logger.debug("Index is consistent with Database on {}", resourceType);
        }
    }

    private void checkDBConsistency(String resourceType) {
        List<String> indexResources = fetchResourceIdsFromIndex(resourceType);
        List<String> databaseResources = fetchResourceIdsFromDB(resourceType);

        List<String> missingDBIds = new ArrayList<>(indexResources);
        missingDBIds.removeAll(databaseResources);
        if (!missingDBIds.isEmpty()) {
            //TODO: Add missing resources to DB
            logger.error("Database is missing the following resources [{}] on {}", missingDBIds, resourceType);
        } else {
            logger.debug("Database is consistent with Index on {}", resourceType);
        }
    }

    private void indexMissingIds(List<String> missingIds, String resourceType) {
        logger.info("Adding {} missing indexes for {}", missingIds.size(), resourceType);
        for (String missingId : missingIds) {
            Resource resource = resourceService.getResource(missingId);
            elasticOperationsService.add(resource);
        }
    }
}