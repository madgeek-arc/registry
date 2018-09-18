package eu.openminted.registry.core.service;

import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.commons.beanutils.PropertyUtils;
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
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xbib.cql.CQLParser;
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator;

import java.util.*;
import java.util.stream.Collectors;


@Service("searchService")
@PropertySource(value = { "classpath:application.properties", "classpath:registry.properties"} )
public class SearchServiceImpl implements SearchService {

    private static Logger logger = LogManager.getLogger(SearchServiceImpl.class);

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


    private Map<String,List<Resource>> buildTopHitAggregation(FacetFilter filter,String category) {
        Map<String,List<Resource>> results = new HashMap<>();
        Client client = elastic.client();
        int quantity = filter.getQuantity();
        BoolQueryBuilder qBuilder = createQueryBuilder(filter);
        SearchRequestBuilder search = client.prepareSearch(filter.getResourceType()).setTypes(type).
                setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qBuilder)
                .setFrom(filter.getFrom()).setSize(0).setExplain(false);
        search.addAggregation(
                AggregationBuilders.terms("agg_category").field(category).size(bucketSize)
                        .subAggregation(AggregationBuilders.topHits("documents").size(topHitsSize)
                        ));
        SearchResponse response = search.execute().actionGet();

        Terms terms = response.getAggregations().get("agg_category");
        for (Bucket bucket : terms.getBuckets()) {
            InternalTopHits hits = bucket.getAggregations().get("documents");

            List<Resource> bucketResults = new ArrayList<>();
            quantity = Math.min(quantity,hits.getHits().getHits().length);
            for(int i = 0 ; i < quantity; ++i) {
                Resource res = new Resource();
                for(String value : Arrays.asList("id","resourceType","payload", "payloadFormat", "version")) {
                    try {
                        PropertyUtils.setProperty(res, value, hits.getHits().getAt(i).getSource().get(value).toString());
                    } catch(Exception e) {
                        break;
                    }
                }
                bucketResults.add(res);
            }
            results.put(bucket.getKeyAsString(), bucketResults);
        }

        return results;
    }

    private Paging<Resource> buildSearch(FacetFilter filter) {
        Client client = elastic.client();

        int quantity = filter.getQuantity();
        BoolQueryBuilder qBuilder = createQueryBuilder(filter);

        SearchRequestBuilder search = client.prepareSearch(filter.getResourceType()).setTypes(type).
                setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qBuilder)
                .setFrom(filter.getFrom()).setSize(quantity).setExplain(false);

        if(filter.getOrderBy() != null) {
            for (Map.Entry<String, Object> order : filter.getOrderBy().entrySet()) {
                Map op = (Map) order.getValue();
                search.addSort(order.getKey(), SortOrder.fromString(op.get("order").toString()));
            }
        }

        for (String browseBy : filter.getBrowseBy()) {
            search.addAggregation(AggregationBuilders.terms("by_" + browseBy).field(browseBy).size(bucketSize));
        }
        SearchResponse response = search.execute().actionGet();

        return responseToPaging(response,filter.getFrom(),filter.getBrowseBy());
    }

    private Facet transformAggregation(String browseBy, Terms terms) {
        Facet facet = new Facet();
        facet.setField(browseBy);
        List<eu.openminted.registry.core.domain.Value> values;
        assert terms.getBuckets() != null && terms.getBuckets().size() > 0;
        values = terms.getBuckets()
                .stream()
                .map(x -> new eu.openminted.registry.core.domain.Value(x.getKeyAsString(),x.getDocCount()))
                .sorted()
                .collect(Collectors.toList());
        facet.setValues(values);
        return facet;
    }

    @Override
    public Paging<Resource> cqlQuery(FacetFilter filter) {

        CQLParser parser = StringUtils.isEmpty(filter.getKeyword()) ? new CQLParser("*") : new CQLParser(filter.getKeyword());

        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator();

        parser.getCQLQuery().accept(generator);

        Client client = elastic.client();
        SearchRequestBuilder search = client.prepareSearch(filter.getResourceType())
                .setTypes(type)
                .setQuery(QueryBuilders.wrapperQuery(generator.getQueryResult()))
                .setSize(filter.getQuantity())
                .setFrom(filter.getFrom())
                .setExplain(false);

        if(filter.getOrderBy() != null) {
            for (Map.Entry<String, Object> order : filter.getOrderBy().entrySet()) {
                Map op = (Map) order.getValue();
                search.addSort(order.getKey(), SortOrder.fromString(op.get("order").toString()));
            }
        }

        for (String browseBy : filter.getBrowseBy()) {
            search.addAggregation(AggregationBuilders.terms("by_" + browseBy).field(browseBy).size(bucketSize));
        }

        SearchResponse response = search.execute().actionGet();

        return responseToPaging(response,filter.getFrom(),filter.getBrowseBy());
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
        SearchRequestBuilder search = client.prepareSearch(resourceType).setTypes(type).setQuery(QueryBuilders.wrapperQuery(generator.getQueryResult()))
                .setSize(quantity)
                .setFrom(from)
                .setExplain(false);

        if(!sortByField.isEmpty()){
            search.addSort(SortBuilders.fieldSort(sortByField).order(SortOrder.valueOf(sortOrder)));
        }

        SearchResponse response = search.execute().actionGet();

        return responseToPaging(response,from, null);

    }

    private Paging<Resource> responseToPaging(SearchResponse response, int from, List<String> browseBy) {
        if (response == null || response.getHits().getTotalHits() == 0) {
            return new Paging<>();
        } else {
            ArrayList<Resource> resources = new ArrayList<>();
            for (SearchHit hit : response.getHits()) {
                String version = hit.getSource().get("version") == null ? "not_set" : hit.getSource().get("version").toString();
                Resource resource = new Resource(
                        hit.getSource().get("id").toString(),
                        resourceTypeService.getResourceType(hit.getSource().get("resourceType").toString()),
                        version,
                        hit.getSource().get("payload").toString(),
                        hit.getSource().get("payloadFormat").toString());

                if(hit.getSource().get("creation_date") != null)
                    resource.setCreationDate(new Date(Long.parseLong(hit.getSource().get("creation_date").toString())));
                if(hit.getSource().get("modification_date") != null)
                    resource.setModificationDate(new Date(Long.parseLong(hit.getSource().get("modification_date").toString())));

                resources.add(resource);
            }
            List<Facet> facets = new ArrayList<>();
            if(browseBy != null) {
                facets = browseBy
                        .stream()
                        .map(x -> transformAggregation(x,response.getAggregations().get("by_" + x)))
                        .collect(Collectors.toList());
            }

            return new Paging<>((int)response.getHits().getTotalHits(),from,from+resources.size(),resources,facets);
        }
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        return cqlQuery(query,resourceType,100,0,"","ASC");
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) throws ServiceException {
        return buildSearch(filter);
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setKeyword(keyword);
        return buildSearch(filter);
    }

    @Override
    public Resource searchId(String resourceType, KeyValue... ids) throws ServiceException {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        //assert that keys are provided
        assert ids.length != 0;

        //iterate all key values and add them to the elastic query
        for(KeyValue kv : ids) {
            qBuilder.must(QueryBuilders.termsQuery(kv.getField(), kv.getValue()));
        }

        Client client = elastic.client();
        SearchRequestBuilder search = client.prepareSearch(resourceType).setTypes(type).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qBuilder)
                .setSize(1).setExplain(false);

        logger.debug("Search query: " + qBuilder + "in index " + resourceType);

        SearchResponse response = search.execute().actionGet();
        if (response == null || response.getHits().getTotalHits() == 0) {
            return null;
        } else {
            SearchHit hit = response.getHits().getAt(0);
            String version = hit.getSource().get("version") == null ? "not_set" : hit.getSource().get("version").toString();
            return new Resource(
                    hit.getSource().get("id").toString(),
                    resourceTypeService.getResourceType(hit.getSource().get("resourceType").toString()),
                    version,
                    hit.getSource().get("payload").toString(),
                    hit.getSource().get("payloadFormat").toString());
        }
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        return buildTopHitAggregation(filter,category);
    }
}
