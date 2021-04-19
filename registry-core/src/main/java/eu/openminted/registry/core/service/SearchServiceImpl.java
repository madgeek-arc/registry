package eu.openminted.registry.core.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.xbib.cql.CQLParser;
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Service("searchService")
@PropertySource(value = {"classpath:application.properties", "classpath:registry.properties"})
public class SearchServiceImpl implements SearchService {

    private static Logger logger = LogManager.getLogger(SearchServiceImpl.class);

    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};

    @Autowired
    Environment environment;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    private RestHighLevelClient restClient;

    @Value("${elastic.aggregation.topHitsSize:100}")
    private int topHitsSize;

    @Value("${elastic.aggregation.bucketSize:100}")
    private int bucketSize;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    private final ObjectMapper mapper;

    public SearchServiceImpl() {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
    }

    @Override
    public BoolQueryBuilder createQueryBuilder(FacetFilter filter) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        if (!filter.getKeyword().equals("")) {
            qBuilder.must(QueryBuilders.matchQuery("searchableArea", filter.getKeyword()));
        } else {
            qBuilder.must(QueryBuilders.matchAllQuery());
        }
        for (Map.Entry<String, Object> filterSet : filter.getFilter().entrySet()) {
            // Check if Filter value is a Collection, and create should matches for every value in the collection.
            // FIXME: Please implement me properly.
            if (Collection.class.isAssignableFrom(filterSet.getValue().getClass())) {
                for (Object value : ((Collection) filterSet.getValue())) {
                    qBuilder.should(QueryBuilders.matchQuery(filterSet.getKey(), value));
                }
                qBuilder.minimumShouldMatch(1);
            } else {
                qBuilder.must(QueryBuilders.termQuery(filterSet.getKey(), filterSet.getValue()));
            }
        }
        return qBuilder;
    }


    private Map<String, List<Resource>> buildTopHitAggregation(FacetFilter filter, String category) {
        Map<String, List<Resource>> results;
        BoolQueryBuilder qBuilder = createQueryBuilder(filter);
        SearchRequest search = new SearchRequest(filter.getResourceType());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        search.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchSourceBuilder.query(qBuilder)
                .fetchSource(INCLUDES, null)
                .from(filter.getFrom()).size(0).explain(false);
        searchSourceBuilder.aggregation(
                AggregationBuilders.terms("agg_category").field(category).size(bucketSize)
                        .subAggregation(AggregationBuilders.topHits("documents").size(topHitsSize).fetchSource(INCLUDES, null)
                        ));
        search.source(searchSourceBuilder);
        SearchResponse response = null;
        try {
            response = restClient.search(search, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }

        Terms terms = response.getAggregations().get("agg_category");
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        results = terms.getBuckets()
                .parallelStream()
                .collect(Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        y -> StreamSupport.stream(
                                ((TopHits) y.getAggregations()
                                        .get("documents"))
                                        .getHits()
                                        .spliterator(), true
                        )
                                .map(r -> {
                                    try {
                                        Resource resource = mapper.readValue(r.getSourceAsString(), Resource.class);
                                        resource.setResourceTypeName(r.getIndex());
                                        return resource;
                                    } catch (IOException e) {
                                        throw new ServiceException(e.getMessage());
                                    }
                                })
                                .collect(Collectors.toList())
                ));

        return results;
    }

    private Paging<Resource> buildSearch(FacetFilter filter) {
        int quantity = filter.getQuantity();
        validateQuantity(quantity);
        BoolQueryBuilder qBuilder = createQueryBuilder(filter);
        SearchRequest search = new SearchRequest(filter.getResourceType()).
                searchType(SearchType.DFS_QUERY_THEN_FETCH);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(qBuilder)
                .fetchSource(INCLUDES, null)
                .from(filter.getFrom()).size(quantity).explain(false);

        if (filter.getOrderBy() != null) {
            for (Map.Entry<String, Object> order : filter.getOrderBy().entrySet()) {
                Map op = (Map) order.getValue();
                searchSourceBuilder.sort(order.getKey(), SortOrder.fromString(op.get("order").toString()));
            }
        }

        for (String browseBy : filter.getBrowseBy()) {
            searchSourceBuilder.aggregation(AggregationBuilders.terms("by_" + browseBy).field(browseBy).size(bucketSize));
        }
        search.source(searchSourceBuilder);
        SearchResponse response = null;
        try {
            response = restClient.search(search, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }

        return responseToPaging(response, filter.getFrom(), filter.getBrowseBy());
    }

    private Facet transformAggregation(String browseBy, Terms terms) {
        Facet facet = new Facet();
        facet.setField(browseBy);
        List<eu.openminted.registry.core.domain.Value> values;
        if (terms.getBuckets() != null && !terms.getBuckets().isEmpty()) {
            values = terms.getBuckets()
                    .stream()
                    .map(x -> new eu.openminted.registry.core.domain.Value(x.getKeyAsString(), x.getDocCount()))
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            values = new ArrayList<>();
        }
        facet.setValues(values);
        return facet;
    }

    @Override
    public Paging<Resource> cqlQuery(FacetFilter filter) {
        validateQuantity(filter.getQuantity());
        CQLParser parser = new CQLParser(filter.getKeyword());
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator();

        parser.getCQLQuery().accept(generator);

        SearchRequest searchRequest = new SearchRequest(filter.getResourceType());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query((QueryBuilders.wrapperQuery(generator.getQueryResult())));
        searchSourceBuilder.size(filter.getQuantity())
                .from(filter.getFrom())
                .fetchSource(INCLUDES, null)
                .explain(true);

        if (filter.getOrderBy() != null) {
            for (Map.Entry<String, Object> order : filter.getOrderBy().entrySet()) {
                Map op = (Map) order.getValue();
                searchSourceBuilder.sort(order.getKey(), SortOrder.fromString(op.get("order").toString()));
            }
        }

        for (String browseBy : filter.getBrowseBy()) {
            searchSourceBuilder.aggregation(AggregationBuilders.terms("by_" + browseBy).field(browseBy).size(bucketSize));
        }
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = null;
        try {
            response = restClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return responseToPaging(response, filter.getFrom(), filter.getBrowseBy());
    }

    @Override
    public Paging<Resource> cqlQuery(String query,
                                     String resourceType,
                                     int quantity,
                                     int from,
                                     String sortByField,
                                     String sortOrder) {
        validateQuantity(quantity);
        CQLParser parser = new CQLParser(query);
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator();

        parser.getCQLQuery().accept(generator);

        SearchRequest searchRequest = new SearchRequest(resourceType);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.fetchSource(INCLUDES, null)
                .query(QueryBuilders.wrapperQuery(generator.getQueryResult()))
                .size(quantity)
                .from(from)
                .explain(false);

        if (!sortByField.isEmpty()) {
            searchSourceBuilder.sort(SortBuilders.fieldSort(sortByField).order(SortOrder.valueOf(sortOrder)));
        }

        searchRequest.source(searchSourceBuilder);
        SearchResponse response = null;
        try {
            response = restClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        return responseToPaging(response, from, null);

    }

    private Paging<Resource> responseToPaging(SearchResponse response, int from, List<String> browseBy) {
        if (response == null || response.getHits().getTotalHits().value == 0) {
            return new Paging<>();
        } else {
            List<Resource> resources = StreamSupport
                    .stream(response.getHits().spliterator(), true)
                    .map(r -> {
                        try {
                            Resource res = mapper.readValue(r.getSourceAsString(), Resource.class);
                            res.setResourceTypeName(r.getIndex());
                            return res;
                        } catch (IOException e) {
                            throw new ServiceException(e.getMessage());
                        }

                    })
                    .collect(Collectors.toList());

            List<Facet> facets = new ArrayList<>();
            if (browseBy != null) {
                facets = browseBy
                        .stream()
                        .map(x -> transformAggregation(x, response.getAggregations().get("by_" + x)))
                        .collect(Collectors.toList());
            }

            return new Paging<>((int) response.getHits().getTotalHits().value, from, from + resources.size(), resources, facets);
        }
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        return cqlQuery(query, resourceType, 100, 0, "", "ASC");
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) {
        return buildSearch(filter);
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setKeyword(keyword);
        return buildSearch(filter);
    }

    @Override
    public Resource searchId(String resourceType, KeyValue... ids) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        //iterate all key values and add them to the elastic query
        Arrays.stream(ids)
                .map(kv -> QueryBuilders.termsQuery(kv.getField(), kv.getValue()))
                .forEach(qBuilder::must);

        SearchRequest searchRequest = new SearchRequest(resourceType);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.fetchSource(INCLUDES, null);
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchSourceBuilder.query(qBuilder)
                .size(1).explain(false);

        searchRequest.source(searchSourceBuilder);
        logger.debug("Search query: " + qBuilder + "in index " + resourceType);
        try {
            SearchResponse searchResponse = restClient.search(searchRequest,RequestOptions.DEFAULT);
            SearchHits ss = searchResponse.getHits();
            Optional<SearchHit> hit = Optional.ofNullable(ss.getTotalHits().value == 0 ? null : ss.getAt(0));

            return hit.map(x -> {
                try {
                    Resource resource = mapper.readValue(x.getSourceAsString(), Resource.class);
                    resource.setResourceTypeName(x.getIndex());
                    return resource;
                } catch (IOException e) {
                    throw new ServiceException(e.getMessage());
                }
            }).orElse(null);
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }

    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        return buildTopHitAggregation(filter, category);
    }

    private void validateQuantity(int quantity) {
        if (quantity > maxQuantity) {
            throw new IllegalArgumentException(String.format("Quantity should be up to %s.", maxQuantity));
        } else if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    static private class ResourcePropertyName extends PropertyNamingStrategy.PropertyNamingStrategyBase {

        @Override
        public String translate(String propertyName) {
            switch (propertyName) {
                case "modificationDate":
                    return "modification_date";
                case "creationDate":
                    return "creation_date";
                default:
                    return propertyName;
            }
        }
    }
}
