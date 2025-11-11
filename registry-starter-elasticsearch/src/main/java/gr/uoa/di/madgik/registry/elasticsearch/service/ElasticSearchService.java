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

package gr.uoa.di.madgik.registry.elasticsearch.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.xbib.cql.CQLParser;
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class ElasticSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);

    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};

    private final RestHighLevelClient elasticsearchClient;
    private final ObjectMapper mapper;
    @Value("${elastic.aggregation.topHitsSize:100}")
    private int topHitsSize;
    @Value("${elastic.aggregation.bucketSize:100}")
    private int bucketSize;
    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public ElasticSearchService(RestHighLevelClient elasticsearchClient) {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
        this.elasticsearchClient = elasticsearchClient;
    }

    public BoolQueryBuilder createQueryBuilder(FacetFilter filter) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        if (!filter.getKeyword().isEmpty()) {
            Set<String> textFields = new HashSet<>(getTextFields(filter.getResourceType()));
            qBuilder.must(QueryBuilders.multiMatchQuery(filter.getKeyword(), textFields.toArray(new String[0])));
        } else {
            qBuilder.must(QueryBuilders.matchAllQuery());
        }
        for (Map.Entry<String, Object> filterSet : filter.getFilter().entrySet()) {
            // Check if Filter value is a Collection, and create should matches for every value in the collection.
            BoolQueryBuilder internalBuilder = new BoolQueryBuilder();
            if (Collection.class.isAssignableFrom(filterSet.getValue().getClass())) {
                for (Object value : ((Collection) filterSet.getValue())) {
                    internalBuilder.should(QueryBuilders.matchQuery(filterSet.getKey(), value));
                }
                internalBuilder.minimumShouldMatch(1);
            } else {
                internalBuilder.must(QueryBuilders.termQuery(filterSet.getKey(), filterSet.getValue()));
            }
            qBuilder.must(internalBuilder);
        }
        return qBuilder;
    }

    private List<String> getTextFields(String indexName) {
        try {
            GetMappingsRequest request = new GetMappingsRequest().indices(indexName);
            GetMappingsResponse response = elasticsearchClient.indices().getMapping(request, RequestOptions.DEFAULT);

            ImmutableOpenMap<String, MappingMetadata> mappingMetaData = response.mappings().get(indexName);
            if (mappingMetaData == null) {
                return Collections.emptyList();
            }

            Map<String, Object> mapping = mappingMetaData.get("_doc").getSourceAsMap();
            Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");

            return findTextFields(properties, "");
        } catch (IOException e) { // fallback to default fields
            logger.warn("Reading resourceType '{}' fields from Elastic failed, using 'searchableArea' and 'payload' instead.", indexName, e);
            return List.of("searchableArea", "payload");
        }
    }

    private List<String> findTextFields(Map<String, Object> properties, String pathPrefix) {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            Map<String, Object> fieldProps = (Map<String, Object>) entry.getValue();
            String fullPath = pathPrefix.isEmpty() ? fieldName : pathPrefix + "." + fieldName;

            Object type = fieldProps.get("type");
            if ("text".equals(type)) {
                result.add(fullPath);
            }

            // Check for nested properties
            if (fieldProps.containsKey("properties")) {
                Map<String, Object> nestedProps = (Map<String, Object>) fieldProps.get("properties");
                result.addAll(findTextFields(nestedProps, fullPath));
            }

            // Check multi-fields
            if (fieldProps.containsKey("fields")) {
                Map<String, Object> subfields = (Map<String, Object>) fieldProps.get("fields");
                for (Map.Entry<String, Object> subfieldEntry : subfields.entrySet()) {
                    String subfieldName = subfieldEntry.getKey();
                    Map<String, Object> subfieldProps = (Map<String, Object>) subfieldEntry.getValue();
                    Object subfieldType = subfieldProps.get("type");
                    if ("text".equals(subfieldType)) {
                        result.add(fullPath + "." + subfieldName);
                    }
                }
            }
        }

        return result;
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
            response = elasticsearchClient.search(search, RequestOptions.DEFAULT);
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

    private Paging<HighlightedResult<Resource>> buildSearchWithHighlights(FacetFilter filter) {
        int quantity = filter.getQuantity();
        validateQuantity(quantity);
        BoolQueryBuilder qBuilder = createQueryBuilder(filter);
        SearchRequest search = new SearchRequest(filter.getResourceType()).
                searchType(SearchType.DFS_QUERY_THEN_FETCH);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(qBuilder)
                .fetchSource(INCLUDES, null)
                .from(filter.getFrom())
                .size(quantity)
                .explain(false);

        HighlightBuilder hb = new HighlightBuilder();
        hb.field("*.analyzed").fragmentSize(2000).numOfFragments(5);
        hb.order("score");
        searchSourceBuilder.highlighter(hb);

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
            response = elasticsearchClient.search(search, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }

        return highlightedResponseToPaging(response, filter.getFrom(), filter.getBrowseBy());
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
            response = elasticsearchClient.search(search, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }

        return responseToPaging(response, filter.getFrom(), filter.getBrowseBy());
    }

    private Facet transformAggregation(String browseBy, Terms terms) {
        Facet facet = new Facet();
        facet.setField(browseBy);
        List<gr.uoa.di.madgik.registry.domain.Value> values;
        if (terms.getBuckets() != null && !terms.getBuckets().isEmpty()) {
            values = terms.getBuckets()
                    .stream()
                    .map(x -> new gr.uoa.di.madgik.registry.domain.Value(x.getKeyAsString(), x.getDocCount()))
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            values = new ArrayList<>();
        }
        facet.setValues(values);
        return facet;
    }

    public Paging<Resource> cqlQuery(FacetFilter filter) throws IOException {
        validateQuantity(filter.getQuantity());
        CQLParser parser = new CQLParser(filter.getKeyword());
        parser.parse();
        ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator(null);

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
            response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
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
        ElasticsearchQueryGenerator generator = null;
        try {
            generator = new ElasticsearchQueryGenerator(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
            response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
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

    private Paging<HighlightedResult<Resource>> highlightedResponseToPaging(SearchResponse response, int from, List<String> browseBy) {
        if (response == null || response.getHits().getTotalHits().value == 0) {
            return new Paging<>();
        } else {
            List<HighlightedResult<Resource>> resources = StreamSupport
                    .stream(response.getHits().spliterator(), true)
                    .map(r -> {
                        try {
                            HighlightedResult<Resource> hr = new HighlightedResult<>();
                            hr.setHighlights(getHighlightsFromMap(r.getHighlightFields()));
                            Resource res = mapper.readValue(r.getSourceAsString(), Resource.class);
                            res.setResourceTypeName(r.getIndex());
                            hr.setResult(res);
                            return hr;
                        } catch (IOException e) {
                            throw new ServiceException(e.getMessage());
                        }

                    })
                    .toList();

            List<Facet> facets = new ArrayList<>();
            if (browseBy != null) {
                facets = browseBy
                        .stream()
                        .map(x -> transformAggregation(x, response.getAggregations().get("by_" + x)))
                        .toList();
            }

            return new Paging<>((int) response.getHits().getTotalHits().value, from, from + resources.size(), resources, facets);
        }
    }

    private List<Highlight> getHighlightsFromMap(Map<String, HighlightField> highlightsMap) {
        List<Highlight> highlights = new ArrayList<>();
        for (Map.Entry<String, HighlightField> hf : highlightsMap.entrySet()) {
            String key = hf.getKey().replace(".analyzed", "");
            for (Text highlight : hf.getValue().fragments()) {
                highlights.add(new Highlight(key, highlight.toString()));
            }
        }
        return highlights;
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
    public Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) {
        return buildSearchWithHighlights(filter);
    }

    @Override
    @Retryable(value = ServiceException.class, backoff = @Backoff(value = 200))
    public Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException {
        logger.debug(String.format("@Retryable 'searchId(resourceType=%s, ids={%s})'", resourceType, String.join(",", Arrays.stream(fields).map(keyValue -> keyValue.getField() + "=" + keyValue.getValue()).collect(Collectors.toSet()))));
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        //iterate all key values and add them to the elastic query
        Arrays.stream(fields)
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
            SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits ss = searchResponse.getHits();
            Optional<SearchHit> hit = Optional.ofNullable(ss.getTotalHits().value == 0 ? null : ss.getAt(0));

            return hit.map(x -> {
                try {
                    Resource resource = mapper.readValue(x.getSourceAsString(), Resource.class);
                    resource.setResourceTypeName(x.getIndex());
                    return resource;
                } catch (IOException e) {
                    logger.debug("@Retryable 'searchId' - ERROR:\n", e);
                    throw new ServiceException(e.getMessage());
                }
            }).orElse(null);
        } catch (IOException e) {
            logger.debug("@Retryable 'searchId' - ERROR:\n", e);
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

    private static class ResourcePropertyName extends PropertyNamingStrategies.NamingBase {

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
