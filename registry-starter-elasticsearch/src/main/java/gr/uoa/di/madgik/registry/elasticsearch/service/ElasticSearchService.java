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

package gr.uoa.di.madgik.registry.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.util.NamedValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.FacetUtils;
import gr.uoa.di.madgik.registry.elasticsearch.autoconfigure.RegistryElasticsearchProperties;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.StringUtils;
import org.xbib.cql.CQLParser;
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch-backed implementation of {@link SearchService}.
 *
 * <p>All queries are issued through the official Elasticsearch Java client
 * ({@code co.elastic.clients:elasticsearch-java}). ObjectNode-based query builders are retained
 * for complex query construction and converted to typed {@link Query} objects via
 * {@link co.elastic.clients.json.WithJson#withJson}.</p>
 */
public class ElasticSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);
    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "created_by", "modified_by", "payloadFormat", "version"};

    private final ElasticsearchClient client;
    private final JacksonJsonpMapper jsonpMapper;
    private final EmbeddingService embeddingService;
    private final ResourceTypeService resourceTypeService;
    private final ElasticIndexFieldsResolver indexFieldsResolver;
    private final ObjectMapper mapper;
    private final int highlightFragmentSize;
    private final int highlightNumberOfFragments;
    private final int topHitsSize;
    private final int bucketSize;
    private final int maxQuantity;

    /**
     * Creates a search service backed by the typed Elasticsearch Java client.
     */
    public ElasticSearchService(ElasticsearchClient client,
                                JacksonJsonpMapper jsonpMapper,
                                EmbeddingService embeddingService,
                                ResourceTypeService resourceTypeService,
                                RegistryElasticsearchProperties elasticsearchProperties,
                                ElasticIndexFieldsResolver indexFieldsResolver) {
        this.client = client;
        this.jsonpMapper = jsonpMapper;
        this.embeddingService = embeddingService;
        this.resourceTypeService = resourceTypeService;
        this.indexFieldsResolver = indexFieldsResolver;
        this.highlightFragmentSize = elasticsearchProperties.getSearch().getHighlight().getFragmentSize();
        this.highlightNumberOfFragments = elasticsearchProperties.getSearch().getHighlight().getNumberOfFragments();
        this.topHitsSize = elasticsearchProperties.getAggregation().getTopHitsSize();
        this.bucketSize = elasticsearchProperties.getAggregation().getBucketSize();
        this.maxQuantity = elasticsearchProperties.getIndex().getMaxResultWindow();
        mapper = new ObjectMapper().findAndRegisterModules();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
    }

    // -------------------------------------------------------------------------
    // Query builders (ObjectNode) — converted to typed Query via toQuery()
    // -------------------------------------------------------------------------

    /**
     * Converts an ObjectNode representing a full ES query into a typed {@link Query}.
     */
    private Query toQuery(ObjectNode node) {
        return Query.of(q -> {
            try {
                return q.withJson(new StringReader(mapper.writeValueAsString(node)));
            } catch (JsonProcessingException e) {
                throw new ServiceException("Failed to build query", e);
            }
        });
    }

    private ObjectNode knnQueryNode(float[] queryVector, int k, float boost, Float similarity) {
        ObjectNode query = mapper.createObjectNode();
        ObjectNode knn = query.putObject("knn");
        knn.put("field", "embedding");
        ArrayNode vector = knn.putArray("query_vector");
        for (float value : queryVector) {
            vector.add(value);
        }
        knn.put("k", k);
        knn.put("num_candidates", knnNumCandidates(k));
        knn.put("boost", boost);
        if (similarity != null) {
            knn.put("similarity", similarity);
        }
        return query;
    }

    private ObjectNode createLexicalQueryNode(FacetFilter filter) {
        ObjectNode bool = mapper.createObjectNode();
        ArrayNode must = bool.putArray("must");

        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            Set<String> textFields = new HashSet<>(resolveTextFields(filter.getResourceType()));
            if (textFields.isEmpty()) {
                // Without any declared text fields there is nowhere to run lexical matching, so a
                // keyword-only query must produce zero lexical hits instead of building an invalid
                // multi_match clause with an empty field list.
                must.addObject().putObject("match_none");
            } else {
                ObjectNode textQuery = must.addObject().putObject("multi_match");
                textQuery.put("query", filter.getKeyword());
                ArrayNode fields = textQuery.putArray("fields");
                textFields.forEach(fields::add);
            }
        } else {
            must.addObject().putObject("match_all");
        }

        applyFilters(filter.getFilter(), bool);
        applyRangeFilters(filter.getRangeFilters(), bool);
        return mapper.createObjectNode().set("bool", bool);
    }

    // Preserved for tests and internal callers that still expect the lexical query builder.
    private ObjectNode createQueryNode(FacetFilter filter) {
        return createLexicalQueryNode(filter);
    }

    /**
     * Builds the hybrid lexical + vector query from the incoming facet filter.
     */
    private ObjectNode createHybridQueryNode(FacetFilter filter) {
        ObjectNode bool = mapper.createObjectNode();
        ArrayNode must = bool.putArray("must");

        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            Set<String> textFields = new HashSet<>(resolveTextFields(filter.getResourceType()));
            ArrayNode should = bool.putArray("should");
            if (!textFields.isEmpty()) {
                ObjectNode textQuery = mapper.createObjectNode();
                ObjectNode multiMatch = textQuery.putObject("multi_match");
                multiMatch.put("query", filter.getKeyword());
                ArrayNode fields = multiMatch.putArray("fields");
                textFields.forEach(fields::add);
                should.add(textQuery);
            }

            float[] embedding = embeddingService.embed(filter.getKeyword());
            if (!embeddingIsEmpty(embedding)) {
                should.add(knnQueryNode(embedding, knnWindow(filter), 2.0f, null));
            }
            if (should.isEmpty()) {
                must.addObject().putObject("match_none");
            } else {
                bool.put("minimum_should_match", 1);
            }
        } else {
            must.addObject().putObject("match_all");
        }

        applyFilters(filter.getFilter(), bool);
        applyRangeFilters(filter.getRangeFilters(), bool);
        return mapper.createObjectNode().set("bool", bool);
    }

    private ObjectNode createSemanticQueryNode(FacetFilter filter) {
        if (!StringUtils.hasText(filter.getKeyword())) {
            throw new ServiceException("Semantic search requires a non-empty keyword.");
        }

        float[] embedding = embeddingService.embed(filter.getKeyword());
        if (embeddingIsEmpty(embedding)) {
            ObjectNode bool = mapper.createObjectNode();
            bool.putArray("must").addObject().putObject("match_none");
            return mapper.createObjectNode().set("bool", bool);
        }

        ObjectNode bool = mapper.createObjectNode();
        ArrayNode must = bool.putArray("must");
        must.add(knnQueryNode(embedding, knnWindow(filter), 1.0f, 0.0f));

        applyFilters(filter.getFilter(), bool);
        applyRangeFilters(filter.getRangeFilters(), bool);
        return mapper.createObjectNode().set("bool", bool);
    }

    private ObjectNode createRecommendationQueryNode(FacetFilter filter, KeyValue resourceIdAndValue, float[] embedding) {
        ObjectNode bool = mapper.createObjectNode();
        ArrayNode must = bool.putArray("must");
        ArrayNode mustNot = bool.putArray("must_not");

        must.add(knnQueryNode(embedding, knnWindow(filter), 1.0f, 0.0f));

        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            Set<String> textFields = new HashSet<>(resolveTextFields(filter.getResourceType()));
            if (!textFields.isEmpty()) {
                ObjectNode textQuery = mapper.createObjectNode();
                ObjectNode multiMatch = textQuery.putObject("multi_match");
                multiMatch.put("query", filter.getKeyword());
                ArrayNode fields = multiMatch.putArray("fields");
                textFields.forEach(fields::add);
                must.add(textQuery);
            }
        }

        mustNot.addObject().putObject("terms")
                .set(resourceIdAndValue.getField(), mapper.createArrayNode().add(resourceIdAndValue.getValue()));

        applyFilters(filter.getFilter(), bool);
        applyRangeFilters(filter.getRangeFilters(), bool);
        return mapper.createObjectNode().set("bool", bool);
    }

    /**
     * Adds exact or multi-value filters to the supplied bool query node.
     */
    private void applyFilters(Map<String, Object> filters, ObjectNode boolNode) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        ArrayNode must = withArray(boolNode, "must");
        for (Map.Entry<String, Object> filterSet : filters.entrySet()) {
            if (filterSet.getValue() instanceof Collection<?> values) {
                ObjectNode internal = mapper.createObjectNode();
                ArrayNode should = internal.putObject("bool").putArray("should");
                for (Object value : values) {
                    ObjectNode match = mapper.createObjectNode();
                    match.putObject("match").set(filterSet.getKey(), mapper.valueToTree(value));
                    should.add(match);
                }
                internal.with("bool").put("minimum_should_match", 1);
                must.add(internal);
            } else {
                ObjectNode term = mapper.createObjectNode();
                term.putObject("term").set(filterSet.getKey(), mapper.valueToTree(filterSet.getValue()));
                must.add(term);
            }
        }
    }

    /**
     * Adds inclusive range filters to the supplied bool query node.
     * When {@link RangeFilter#isIncludeNull()} is true, records missing the field also pass.
     */
    private void applyRangeFilters(Map<String, RangeFilter> rangeFilters, ObjectNode boolNode) {
        if (rangeFilters == null || rangeFilters.isEmpty()) {
            return;
        }
        ArrayNode must = withArray(boolNode, "must");
        for (Map.Entry<String, RangeFilter> entry : rangeFilters.entrySet()) {
            String field = entry.getKey();
            RangeFilter rf = entry.getValue();
            boolean hasBounds = rf.getFrom() != null || rf.getTo() != null;

            if (!hasBounds) {
                if (rf.isIncludeNull()) {
                    // No bounds but caller wants null-field records → match only docs missing the field
                    ObjectNode missingOnly = mapper.createObjectNode();
                    missingOnly.putObject("bool").putObject("must_not").putObject("exists").put("field", field);
                    must.add(missingOnly);
                }
                // Both null + !includeNull → no constraint; adding range:{} would be a match-all, so skip
                continue;
            }

            ObjectNode rangeQuery = mapper.createObjectNode();
            ObjectNode rangeClause = rangeQuery.putObject("range").putObject(field);
            if (rf.getFrom() != null) rangeClause.set("gte", mapper.valueToTree(rf.getFrom()));
            if (rf.getTo() != null)   rangeClause.set("lte", mapper.valueToTree(rf.getTo()));

            if (rf.isIncludeNull()) {
                // ES equivalent of: field IS NULL OR field IN [from, to]
                // bool/should with minimum_should_match=1 so either branch satisfies the filter
                ObjectNode shouldWrapper = mapper.createObjectNode();
                ArrayNode should = shouldWrapper.putObject("bool").putArray("should");
                ObjectNode missingClause = mapper.createObjectNode();
                missingClause.putObject("bool").putObject("must_not").putObject("exists").put("field", field);
                should.add(missingClause);
                should.add(rangeQuery);
                ((ObjectNode) shouldWrapper.get("bool")).put("minimum_should_match", 1);
                must.add(shouldWrapper);
            } else {
                must.add(rangeQuery);
            }
        }
    }

    /**
     * Returns an existing array node or creates it when absent.
     */
    private ArrayNode withArray(ObjectNode objectNode, String field) {
        JsonNode existing = objectNode.get(field);
        if (existing instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        return objectNode.putArray(field);
    }

    // -------------------------------------------------------------------------
    // Sort / aggregation helpers
    // -------------------------------------------------------------------------

    private List<SortOptions> buildSorts(Map<String, Object> orderBy) {
        if (orderBy == null) return List.of();
        return orderBy.entrySet().stream()
                .map(e -> {
                    Map<?, ?> op = (Map<?, ?>) e.getValue();
                    SortOrder order = "asc".equalsIgnoreCase(op.get("order").toString())
                            ? SortOrder.Asc : SortOrder.Desc;
                    return SortOptions.of(so -> so.field(f -> f.field(e.getKey()).order(order)));
                }).collect(Collectors.toList());
    }

    private Map<String, Aggregation> buildAggregations(List<String> browseBy) {
        if (browseBy == null) return Map.of();
        return browseBy.stream().collect(Collectors.toMap(
                field -> "by_" + field,
                field -> Aggregation.of(a -> a.terms(t -> t.field(field).size(bucketSize)))
        ));
    }

    // -------------------------------------------------------------------------
    // Search execution
    // -------------------------------------------------------------------------

    private Paging<Resource> buildSearch(FacetFilter filter, ObjectNode queryNode) {
        filter.setBrowseBy(resolveBrowseBy(filter));
        int quantity = filter.getQuantity();
        validateQuantity(quantity);

        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(filter.getResourceType())
                            .searchType(SearchType.DfsQueryThenFetch)
                            .query(toQuery(queryNode))
                            .source(src -> src.filter(f -> f.includes(List.of(INCLUDES))))
                            .from(filter.getFrom())
                            .size(quantity)
                            .trackTotalHits(t -> t.enabled(true))
                            .sort(buildSorts(filter.getOrderBy()))
                            .aggregations(buildAggregations(filter.getBrowseBy())),
                    ObjectNode.class);
            return responseToPaging(response, filter.getFrom(), filter.getBrowseBy(), filter.getResourceType());
        } catch (IOException e) {
            throw new ServiceException("Search failed", e);
        }
    }

    private Paging<HighlightedResult<Resource>> buildSearchWithHighlights(FacetFilter filter, ObjectNode queryNode) {
        filter.setBrowseBy(resolveBrowseBy(filter));
        int quantity = filter.getQuantity();
        validateQuantity(quantity);

        try {
            List<NamedValue<HighlightField>> highlightFields = resolveTextFields(filter.getResourceType()).stream()
                    .map(field -> NamedValue.of(field,
                            HighlightField.of(hf -> hf
                                    .fragmentSize(highlightFragmentSize)
                                    .numberOfFragments(highlightNumberOfFragments))))
                    .toList();
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(filter.getResourceType())
                            .searchType(SearchType.DfsQueryThenFetch)
                            .query(toQuery(queryNode))
                            .source(src -> src.filter(f -> f.includes(List.of(INCLUDES))))
                            .from(filter.getFrom())
                            .size(quantity)
                            .trackTotalHits(t -> t.enabled(true))
                            .sort(buildSorts(filter.getOrderBy()))
                            .aggregations(buildAggregations(filter.getBrowseBy()))
                            .highlight(h -> h
                                    .order(HighlighterOrder.Score)
                                    .fields(highlightFields)),
                    ObjectNode.class);
            return highlightedResponseToPaging(response, filter.getFrom(), filter.getBrowseBy(), filter.getResourceType());
        } catch (IOException e) {
            throw new ServiceException("Search with highlights failed", e);
        }
    }

    private Map<String, List<Resource>> buildTopHitAggregation(FacetFilter filter, String category) {
        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(filter.getResourceType())
                            .searchType(SearchType.DfsQueryThenFetch)
                            .query(toQuery(createLexicalQueryNode(filter)))
                            .source(src -> src.fetch(false))
                            .size(0)
                            .trackTotalHits(t -> t.enabled(true))
                            .aggregations("agg_category", a -> a
                                    .terms(t -> t.field(category).size(bucketSize))
                                    .aggregations("documents", da -> da.topHits(th -> th
                                            .size(topHitsSize)
                                            .source(src -> src.filter(f -> f.includes(List.of(INCLUDES))))))),
                    ObjectNode.class);

            Map<String, List<Resource>> results = new HashMap<>();
            Aggregate agg = response.aggregations().get("agg_category");
            if (agg != null && agg.isSterms()) {
                for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
                    List<Resource> bucketResources = bucket.aggregations().get("documents")
                            .topHits().hits().hits().stream()
                            .map(this::jsonDataHitToResource)
                            .collect(Collectors.toList());
                    results.put(bucket.key().stringValue(), bucketResources);
                }
            }
            return results;
        } catch (IOException e) {
            throw new ServiceException("Top-hit aggregation failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // Response conversion helpers
    // -------------------------------------------------------------------------

    private Resource toResource(Hit<ObjectNode> hit) {
        try {
            Resource resource = mapper.treeToValue(hit.source(), Resource.class);
            resource.setResourceTypeName(hit.index());
            return resource;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    private Resource jsonDataHitToResource(Hit<JsonData> hit) {
        StringWriter writer = new StringWriter();
        try (jakarta.json.stream.JsonGenerator gen = jsonpMapper.jsonProvider().createGenerator(writer)) {
            hit.source().serialize(gen, jsonpMapper);
        }
        try {
            Resource resource = mapper.readValue(writer.toString(), Resource.class);
            resource.setResourceTypeName(hit.index());
            return resource;
        } catch (IOException e) {
            throw new ServiceException("Failed to deserialize resource", e);
        }
    }

    private Paging<Resource> responseToPaging(SearchResponse<ObjectNode> response, int from,
                                              List<String> browseBy, String resourceTypeName) {
        List<Hit<ObjectNode>> hits = response.hits().hits();
        List<Facet> facets = createFacets(browseBy, resourceTypeName, response.aggregations());
        if (hits.isEmpty()) {
            return new Paging<>(extractTotal(response), from, from, List.of(), facets);
        }

        List<Resource> resources = hits.stream().map(this::toResource).collect(Collectors.toList());

        return new Paging<>(extractTotal(response), from, from + resources.size(), resources, facets);
    }

    private Paging<HighlightedResult<Resource>> highlightedResponseToPaging(
            SearchResponse<ObjectNode> response, int from,
            List<String> browseBy, String resourceTypeName) {

        List<Hit<ObjectNode>> hits = response.hits().hits();
        List<Facet> facets = createFacets(browseBy, resourceTypeName, response.aggregations());
        if (hits.isEmpty()) {
            return new Paging<>(extractTotal(response), from, from, List.of(), facets);
        }

        List<HighlightedResult<Resource>> resources = new ArrayList<>();
        for (Hit<ObjectNode> hit : hits) {
            Resource resource = toResource(hit);
            HighlightedResult<Resource> result = new HighlightedResult<>();
            List<Highlight> highlights = new ArrayList<>();
            hit.highlight().forEach((key, fragments) -> {
                fragments.forEach(frag -> highlights.add(
                        // remove .text from highligh field name
                        new Highlight(key.replace(".text", ""), frag)));
            });
            result.setHighlights(highlights);
            result.setResult(resource);
            result.setScore(hit.score() != null ? hit.score().floatValue() : 0.0f);
            resources.add(result);
        }

        return new Paging<>(extractTotal(response), from, from + resources.size(), resources, facets);
    }

    private List<Facet> createFacets(List<String> browseBy, String resourceTypeName,
                                     Map<String, Aggregate> aggregations) {
        Map<String, String> fieldLabels = resourceTypeService.getIndexFieldLabels(resourceTypeName);
        return FacetUtils.createFacets(
                browseBy,
                fieldLabels::get,
                field -> aggregationValues(field, aggregations)
        );
    }

    private List<gr.uoa.di.madgik.registry.domain.Value> aggregationValues(String browseBy, Map<String, Aggregate> aggregations) {
        List<gr.uoa.di.madgik.registry.domain.Value> values = new ArrayList<>();
        Aggregate agg = aggregations != null ? aggregations.get("by_" + browseBy) : null;
        if (agg != null && agg.isSterms()) {
            for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
                values.add(new gr.uoa.di.madgik.registry.domain.Value(
                        bucket.key().stringValue(), bucket.docCount()));
            }
        }
        return FacetUtils.normalizeValues(values);
    }

    private int extractTotal(SearchResponse<?> response) {
        TotalHits total = response.hits().total();
        return total != null ? (int) total.value() : 0;
    }

    // -------------------------------------------------------------------------
    // Text-field discovery via mapping API
    // -------------------------------------------------------------------------

    private List<String> resolveTextFields(String indexName) {
        return indexFieldsResolver.getTextFields(indexName);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<String> resolveBrowseBy(FacetFilter filter) {
        ResourceType rt = resourceTypeService.getResourceType(filter.getResourceType());
        List<ResourceType> resourceTypes = rt != null
                ? List.of(rt)
                : resourceTypeService.getAllResourceTypeByAlias(filter.getResourceType());
        return SearchService.resolveBrowseBy(resourceTypes, filter.getBrowseBy());
    }

    private void validateQuantity(int quantity) {
        if (quantity > maxQuantity) {
            throw new IllegalArgumentException(String.format("Quantity should be up to %s.", maxQuantity));
        } else if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    private boolean embeddingIsEmpty(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return true;
        }
        boolean hasNonZero = false;
        for (float x : embedding) {
            if (!Float.isFinite(x)) {
                return true;   // any ±Infinity or NaN → reject the whole vector
            }
            if (x != 0) {
                hasNonZero = true;
            }
        }
        return !hasNonZero;
    }

    private int knnWindow(FacetFilter filter) {
        return Math.max(1, Math.min(maxQuantity, filter.getFrom() + Math.max(filter.getQuantity(), 1)));
    }

    private int knnNumCandidates(int k) {
        return Math.min(maxQuantity, Math.max(k * 2, 50));
    }

    private float[] getEmbeddingForResource(String resourceType, KeyValue resourceIdAndValue) {
        try {
            List<FieldValue> fieldValues = List.of(FieldValue.of(resourceIdAndValue.getValue()));
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(resourceType)
                            .size(1)
                            .source(src -> src.filter(f -> f.includes("embedding")))
                            .trackTotalHits(t -> t.enabled(true))
                            .query(q -> q.terms(t -> t.field(resourceIdAndValue.getField())
                                    .terms(tv -> tv.value(fieldValues)))),
                    ObjectNode.class);
            List<Hit<ObjectNode>> hits = response.hits().hits();
            if (hits.isEmpty()) {
                throw new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException(
                        "There are no recommendations available for this resource",
                        new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException("Could not find resource"));
            }
            ObjectNode source = hits.get(0).source();
            if (source == null) {
                throw new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException(
                        "There are no recommendations available for this resource",
                        new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException("Embedding field missing"));
            }
            JsonNode embeddingNode = source.get("embedding");
            if (embeddingNode == null || !embeddingNode.isArray()) {
                throw new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException(
                        "There are no recommendations available for this resource",
                        new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException("Embedding field missing"));
            }
            return mapper.convertValue(embeddingNode, float[].class);
        } catch (IOException e) {
            throw new ServiceException("Failed to get embedding", e);
        }
    }

    // -------------------------------------------------------------------------
    // SearchService interface implementation
    // -------------------------------------------------------------------------

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
        ElasticsearchQueryGenerator generator;
        try {
            generator = new ElasticsearchQueryGenerator(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        parser.getCQLQuery().accept(generator);
        String cqlQueryJson = generator.getQueryResult();

        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(resourceType)
                            .searchType(SearchType.DfsQueryThenFetch)
                            .query(q -> q.withJson(new StringReader(cqlQueryJson)))
                            .source(src -> src.filter(f -> f.includes(List.of(INCLUDES))))
                            .from(from)
                            .size(quantity)
                            .trackTotalHits(t -> t.enabled(true))
                            .sort(sortByField.isEmpty() ? List.of() : List.of(
                                    SortOptions.of(so -> so.field(f -> f.field(sortByField)
                                                                       .order("asc".equalsIgnoreCase(sortOrder) ? SortOrder.Asc : SortOrder.Desc))))),
                    ObjectNode.class);
            return responseToPaging(response, from, null, null);
        } catch (IOException e) {
            throw new ServiceException("CQL search failed", e);
        }
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        return cqlQuery(query, resourceType, 100, 0, "", "ASC");
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) {
        return buildSearch(filter, createLexicalQueryNode(filter));
    }

    @Override
    public Paging<Resource> semanticSearch(FacetFilter filter) {
        return buildSearch(filter, createSemanticQueryNode(filter));
    }

    @Override
    public Paging<Resource> hybridSearch(FacetFilter filter) {
        return buildSearch(filter, createHybridQueryNode(filter));
    }

    @Override
    public List<Resource> recommend(FacetFilter filter, KeyValue resourceIdAndValue) {
        int quantity = filter.getQuantity();
        validateQuantity(quantity);

        float[] embedding = getEmbeddingForResource(filter.getResourceType(), resourceIdAndValue);
        if (embeddingIsEmpty(embedding)) {
            throw new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException(
                    "There are no recommendations available for this resource",
                    new UnsupportedOperationException("Embedding value is empty, cannot find recommendations")
            );
        }

        Query query = toQuery(createRecommendationQueryNode(filter, resourceIdAndValue, embedding));

        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(filter.getResourceType())
                            .searchType(SearchType.DfsQueryThenFetch)
                            .query(query)
                            .source(src -> src.filter(f -> f.includes(List.of(INCLUDES))))
                            .from(filter.getFrom())
                            .size(quantity)
                            .trackTotalHits(t -> t.enabled(true)),
                    ObjectNode.class);
            return response.hits().hits().stream().map(this::toResource).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ServiceException("Recommend search failed", e);
        }
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setKeyword(keyword);
        return search(filter);
    }

    @Override
    public Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) {
        return buildSearchWithHighlights(filter, createLexicalQueryNode(filter));
    }

    @Override
    public Paging<HighlightedResult<Resource>> hybridSearchWithHighlights(FacetFilter filter) {
        return buildSearchWithHighlights(filter, createHybridQueryNode(filter));
    }

    @Override
    @Retryable(retryFor = ServiceException.class, backoff = @Backoff(value = 200))
    public Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException {
        logger.debug(String.format("@Retryable 'searchId(resourceType=%s, ids={%s})'", resourceType,
                String.join(",", Arrays.stream(fields)
                        .map(keyValue -> keyValue.getField() + "=" + keyValue.getValue())
                        .collect(Collectors.toSet()))));

        List<Query> musts = Arrays.stream(fields)
                .map(kv -> Query.of(q -> q.terms(t -> t.field(kv.getField())
                        .terms(tv -> tv.value(List.of(FieldValue.of(kv.getValue())))))))
                .collect(Collectors.toList());

        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(resourceType)
                            .searchType(SearchType.DfsQueryThenFetch)
                            .query(q -> q.bool(b -> b.must(musts)))
                            .source(src -> src.filter(f -> f.includes(List.of(INCLUDES))))
                            .size(1)
                            .trackTotalHits(t -> t.enabled(true)),
                    ObjectNode.class);
            List<Hit<ObjectNode>> hits = response.hits().hits();
            return hits.isEmpty() ? null : toResource(hits.get(0));
        } catch (IOException e) {
            throw new ServiceException("searchFields failed", e);
        }
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        return buildTopHitAggregation(filter, category);
    }

    @Override
    public Map<String, String> getLabels(String resourceType, String idField,
                                         List<String> ids, String labelField) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<FieldValue> fieldValues = ids.stream().map(FieldValue::of).collect(Collectors.toList());
        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                            .index(resourceType)
                            .query(q -> q.terms(t -> t.field(idField).terms(tv -> tv.value(fieldValues))))
                            .source(src -> src.filter(f -> f.includes(idField, labelField)))
                            .size(ids.size())
                            .trackTotalHits(t -> t.enabled(true)),
                    ObjectNode.class);
            Map<String, String> result = new HashMap<>();
            for (Hit<ObjectNode> hit : response.hits().hits()) {
                ObjectNode source = hit.source();
                if (source == null) continue;
                JsonNode id = source.get(idField);
                JsonNode label = source.get(labelField);
                if (id != null && label != null) {
                    result.put(id.asText(), label.asText());
                }
            }
            return result;
        } catch (IOException e) {
            throw new ServiceException("getLabels failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // Inner naming strategy
    // -------------------------------------------------------------------------

    private static class ResourcePropertyName extends PropertyNamingStrategies.NamingBase {

        @Override
        public String translate(String propertyName) {
            return switch (propertyName) {
                case "modificationDate" -> "modification_date";
                case "creationDate" -> "creation_date";
                case "createdBy" -> "created_by";
                case "modifiedBy" -> "modified_by";
                default -> propertyName;
            };
        }
    }
}
