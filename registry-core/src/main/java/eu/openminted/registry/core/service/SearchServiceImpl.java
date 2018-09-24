package eu.openminted.registry.core.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.xbib.cql.CQLParser;
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Service("searchService")
@PropertySource(value = {"classpath:application.properties", "classpath:registry.properties"})
public class SearchServiceImpl implements SearchService {

    private static Logger logger = LogManager.getLogger(SearchServiceImpl.class);

    private static final String[] INCLUDES = {"payload", "creation_date", "modification_date", "payloadFormat", "version"};

    @Autowired
    Environment environment;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    private ElasticConfiguration elastic;

    @Value("${elastic.aggregation.topHitsSize:100}")
    private int topHitsSize;

    @Value("${elastic.aggregation.bucketSize:100}")
    private int bucketSize;

    @Value("${prefix:general}")
    private String type;

    SearchServiceImpl() {
    }


    private static BoolQueryBuilder createQueryBuilder(FacetFilter filter) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        if (!filter.getKeyword().equals("")) {
            qBuilder.must(QueryBuilders.matchQuery("searchableArea", filter.getKeyword()));
        } else {
            qBuilder.must(QueryBuilders.matchAllQuery());
        }
        for (Map.Entry<String, Object> filterSet : filter.getFilter().entrySet()) {
            qBuilder.must(QueryBuilders.termQuery(filterSet.getKey(), filterSet.getValue()));
        }
        return qBuilder;
    }


    private Map<String, List<Resource>> buildTopHitAggregation(FacetFilter filter, String category) {
        Map<String, List<Resource>> results;
        Client client = elastic.client();
        BoolQueryBuilder qBuilder = createQueryBuilder(filter);
        SearchRequestBuilder search = client.prepareSearch(filter.getResourceType()).setTypes(type).
                setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qBuilder)
                .setFetchSource(INCLUDES, null)
                .setFrom(filter.getFrom()).setSize(0).setExplain(false);
        search.addAggregation(
                AggregationBuilders.terms("agg_category").field(category).size(bucketSize)
                        .subAggregation(AggregationBuilders.topHits("documents").size(topHitsSize).fetchSource(INCLUDES, null)
                        ));
        SearchResponse response = search.execute().actionGet();

        Terms terms = response.getAggregations().get("agg_category");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        results = terms.getBuckets()
                .parallelStream()
                .collect(Collectors.toMap(
                        MultiBucketsAggregation.Bucket::getKeyAsString,
                        y -> StreamSupport.stream(
                                ((InternalTopHits) y.getAggregations()
                                        .get("documents"))
                                        .getHits()
                                        .spliterator(), true
                        )
                                .map(r -> mapper.convertValue(r.getSource(), Resource.class))
                                .collect(Collectors.toList())
                ));

        return results;
    }

    private Paging<Resource> buildSearch(FacetFilter filter) {
        Client client = elastic.client();

        int quantity = filter.getQuantity();
        BoolQueryBuilder qBuilder = createQueryBuilder(filter);

        SearchRequestBuilder search = client.prepareSearch(filter.getResourceType()).setTypes(type).
                setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qBuilder)
                .setFetchSource(INCLUDES, null)
                .setFrom(filter.getFrom()).setSize(quantity).setExplain(false);

        if (filter.getOrderBy() != null) {
            for (Map.Entry<String, Object> order : filter.getOrderBy().entrySet()) {
                Map op = (Map) order.getValue();
                search.addSort(order.getKey(), SortOrder.fromString(op.get("order").toString()));
            }
        }

        for (String browseBy : filter.getBrowseBy()) {
            search.addAggregation(AggregationBuilders.terms("by_" + browseBy).field(browseBy).size(bucketSize));
        }
        SearchResponse response = search.execute().actionGet();

        return responseToPaging(response, filter.getFrom(), filter.getBrowseBy());
    }

    private Facet transformAggregation(String browseBy, Terms terms) {
        Facet facet = new Facet();
        facet.setField(browseBy);
        List<eu.openminted.registry.core.domain.Value> values;
        assert terms.getBuckets() != null && !terms.getBuckets().isEmpty();
        values = terms.getBuckets()
                .stream()
                .map(x -> new eu.openminted.registry.core.domain.Value(x.getKeyAsString(), x.getDocCount()))
                .sorted()
                .collect(Collectors.toList());
        facet.setValues(values);
        return facet;
    }

    @Override
    public Paging<Resource> cqlQuery(FacetFilter filter) {

        CQLParser parser = new CQLParser(filter.getKeyword());
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator();

        parser.getCQLQuery().accept(generator);

        Client client = elastic.client();
        SearchRequestBuilder search = client.prepareSearch(filter.getResourceType())
                .setTypes(type)
                .setQuery(QueryBuilders.wrapperQuery(generator.getQueryResult()))
                .setSize(filter.getQuantity())
                .setFrom(filter.getFrom())
                .setFetchSource(INCLUDES, null)
                .setExplain(true);

        if (filter.getOrderBy() != null) {
            for (Map.Entry<String, Object> order : filter.getOrderBy().entrySet()) {
                Map op = (Map) order.getValue();
                search.addSort(order.getKey(), SortOrder.fromString(op.get("order").toString()));
            }
        }

        for (String browseBy : filter.getBrowseBy()) {
            search.addAggregation(AggregationBuilders.terms("by_" + browseBy).field(browseBy).size(bucketSize));
        }
        SearchResponse response = search.execute().actionGet();

        return responseToPaging(response, filter.getFrom(), filter.getBrowseBy());
    }

    @Override
    public Paging<Resource> cqlQuery(String query,
                                     String resourceType,
                                     int quantity,
                                     int from,
                                     String sortByField,
                                     String sortOrder) {

        CQLParser parser = new CQLParser(query);
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator();

        parser.getCQLQuery().accept(generator);

        Client client = elastic.client();
        SearchRequestBuilder search = client
                .prepareSearch(resourceType)
                .setTypes(type)
                .setFetchSource(INCLUDES, null)
                .setQuery(QueryBuilders.wrapperQuery(generator.getQueryResult()))
                .setSize(quantity)
                .setFrom(from)
                .setExplain(false);

        if (!sortByField.isEmpty()) {
            search.addSort(SortBuilders.fieldSort(sortByField).order(SortOrder.valueOf(sortOrder)));
        }

        SearchResponse response = search.execute().actionGet();

        return responseToPaging(response, from, null);

    }

    private Paging<Resource> responseToPaging(SearchResponse response, int from, List<String> browseBy) {
        if (response == null || response.getHits().getTotalHits() == 0) {
            return new Paging<>();
        } else {
            ObjectMapper mapper = new ObjectMapper();
            List<Resource> resources = StreamSupport
                    .stream(response.getHits().spliterator(), true)
                    .map(r -> mapper.convertValue(r.getSource(), Resource.class))
                    .collect(Collectors.toList());

            List<Facet> facets = new ArrayList<>();
            if (browseBy != null) {
                facets = browseBy
                        .stream()
                        .map(x -> transformAggregation(x, response.getAggregations().get("by_" + x)))
                        .collect(Collectors.toList());
            }

            return new Paging<>((int) response.getHits().getTotalHits(), from, from + resources.size(), resources, facets);
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
        //assert that keys are provided
        assert ids.length != 0;

        //iterate all key values and add them to the elastic query
        for (KeyValue kv : ids) {
            qBuilder.must(QueryBuilders.termsQuery(kv.getField(), kv.getValue()));
        }

        Client client = elastic.client();
        SearchRequestBuilder search = client
                .prepareSearch(resourceType)
                .setTypes(type)
                .setFetchSource(INCLUDES,null)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qBuilder)
                .setSize(1).setExplain(false);

        logger.debug("Search query: " + qBuilder + "in index " + resourceType);

        SearchResponse response = search.execute().actionGet();
        if (response == null || response.getHits().getTotalHits() == 0) {
            return null;
        } else {
            SearchHit hit = response.getHits().getAt(0);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(hit.getSource(),Resource.class);
//            String version = hit.getSource().get("version") == null ? "not_set" : hit.getSource().get("version").toString();
//            return new Resource(
//                    hit.getSource().get("id").toString(),
//                    null,
//                    version,
//                    hit.getSource().get("payload").toString(),
//                    hit.getSource().get("payloadFormat").toString());
        }
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        return buildTopHitAggregation(filter, category);
    }
}
