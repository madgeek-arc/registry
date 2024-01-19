package eu.openminted.registry.core;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

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

    //    @Scheduled(cron = "0 0 0 * * *") // At midnight every day
//    @Scheduled(fixedDelay = 5 * 60 * 1000)
    private void scheduledValidation() {
        List<String> resourceTypeNames = resourceTypeService.getAllResourceType(0, 100)
                .stream().map(ResourceType::getName).collect(Collectors.toList());
        //TODO: use elastic scroll if we need to check events too (>10k)
        resourceTypeNames.remove("event");
        for (String resourceTypeName : resourceTypeNames) {
            validate(resourceTypeName, true);
            validate(resourceTypeName, false);
        }
    }

    public void validate(String resourceType, boolean validateDBtoElastic) {
        // retrieve from DB
        List<String> databaseResources = fetchResourceIdsFromDB(resourceType);
        // retrieve from Elastic
        List<String> elasticResources = fetchResourceIdsFromElastic(resourceType);

        validateDBAndElasticEntries(databaseResources, elasticResources, resourceType, validateDBtoElastic);
    }

    private List<String> fetchResourceIdsFromDB(String resourceType) {
        List<String> databaseResources = new ArrayList<>();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();

        String query = "SELECT id FROM " + resourceType + "_view";

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);
        if (records != null && !records.isEmpty()) {
            for (Map<String, Object> record : records) {
                databaseResources.add((String) record.get("id"));
            }
        }
        return databaseResources;
    }

    private List<String> fetchResourceIdsFromElastic(String resourceType) {
        List<String> resourceIds = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .from(0)
                .size(10000)
                .docValueField("*_id")
                .fetchSource(false)
                .explain(true)
                .query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("_index", resourceType)));
        searchRequest.source(searchSourceBuilder);

        SearchResponse response;
        try {
            response = client.search(searchRequest, RequestOptions.DEFAULT);
            List<SearchHit> hits = Arrays.stream(response.getHits().getHits()).collect(Collectors.toList());
            for (SearchHit hit : hits) {
                resourceIds.add(hit.getFields().get("_id").getValue());
            }
        } catch (IOException e) {
            logger.error("Error retrieving _id value from Elastic.", e);
        }
        return resourceIds;
    }

    private void validateDBAndElasticEntries(List<String> databaseResources, List<String> elasticResources,
                                             String resourceType, boolean validateDBtoElastic) {
        if (validateDBtoElastic) {
            List<String> missingElasticIds = new ArrayList<>(databaseResources);
            missingElasticIds.removeAll(elasticResources);
            if (!missingElasticIds.isEmpty()) {
                indexMissingElasticIds(missingElasticIds, resourceType);
            } else {
                logger.info("Elastic is consistent with Database on {}", resourceType);
            }
        } else {
            List<String> missingDBIds = new ArrayList<>(elasticResources);
            missingDBIds.removeAll(databaseResources);
            if (!missingDBIds.isEmpty()) {
                //TODO: Add missing resources to DB
                logger.info("Database is missing the following resources {} on {}", missingDBIds, resourceType);
            } else {
                logger.info("Database is consistent with Elastic on {}", resourceType);
            }
        }
    }

    private void indexMissingElasticIds(List<String> missingElasticIds, String resourceType) {
        logger.info("Adding {} missing indexes for {}", missingElasticIds.size(), resourceType);
        for (String missingElasticId : missingElasticIds) {
            Resource resource = resourceService.getResource(missingElasticId);
            elasticOperationsService.add(resource);
        }
    }
}