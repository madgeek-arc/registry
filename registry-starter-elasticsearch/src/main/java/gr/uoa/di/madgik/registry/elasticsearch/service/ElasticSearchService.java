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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Highlight;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.xbib.cql.CQLParser;
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import gr.uoa.di.madgik.registry.elasticsearch.ElasticRestUtils;

/**
 * Elasticsearch-backed implementation of {@link SearchService}.
 *
 * <p>The service keeps the existing registry search semantics but renders all queries,
 * aggregations, highlighting, and recommendation requests as raw Elasticsearch JSON. This avoids
 * the removed 7.x client APIs while preserving the public search contract used by the rest of the
 * application.</p>
 */
public class ElasticSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);
    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};

    private final RestClient elasticsearchClient;
    private final EmbeddingService embeddingService;
    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper;
    @Value("${elastic.aggregation.topHitsSize:100}")
    private int topHitsSize;
    @Value("${elastic.aggregation.bucketSize:100}")
    private int bucketSize;
    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;


    /**
     * Creates a search service backed by Elasticsearch's low-level REST client.
     */
    public ElasticSearchService(RestClient elasticsearchClient,
                                EmbeddingService embeddingService,
                                ResourceTypeService resourceTypeService) {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingService = embeddingService;
        this.resourceTypeService = resourceTypeService;
    }

    /**
     * Builds the painless script used to blend text relevance with embedding similarity.
     */
    private ObjectNode cosineScriptScoreQuery(float[] queryVector) {
        ObjectNode script = mapper.createObjectNode();
        ObjectNode params = script.putObject("params");
        ArrayNode q = params.putArray("q");
        for (float value : queryVector) {
            q.add(value);
        }
        params.put("text_w", 1.0);
        params.put("vec_w", 2.0);
        script.put("source", """
                            double text = _score;
                            if (!doc.containsKey('embedding') || doc['embedding'].size() == 0) {
                                return text;
                            }
                            double vec = cosineSimilarity(params.q, doc['embedding']) + 1.0;
                            return params.text_w * text + params.vec_w * vec;
                        """);
        return script;
    }

    /**
     * Builds the main boolean query from the incoming facet filter.
     */
    private ObjectNode createQueryNode(FacetFilter filter) {
        ObjectNode bool = mapper.createObjectNode();
        ArrayNode must = bool.putArray("must");

        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            Set<String> textFields = new HashSet<>(getTextFields(filter.getResourceType()));
            ObjectNode scriptScore = mapper.createObjectNode();
            ObjectNode scriptScoreBody = scriptScore.putObject("script_score");
            ObjectNode multiMatch = scriptScoreBody.putObject("query").putObject("multi_match");
            multiMatch.put("query", filter.getKeyword());
            ArrayNode fields = multiMatch.putArray("fields");
            textFields.forEach(fields::add);
            scriptScoreBody.set("script", cosineScriptScoreQuery(embeddingService.embed(filter.getKeyword())));
            must.add(scriptScore);
        } else {
            must.addObject().putObject("match_all");
        }

        applyFilters(filter.getFilter(), bool);
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
     * Reads the current index mapping and returns every field that is searchable as text.
     */
    private List<String> getTextFields(String indexName) {
        try {
            JsonNode response = ElasticRestUtils.performJsonRequest(
                    elasticsearchClient,
                    mapper,
                    "GET",
                    "/" + indexName + "/_mapping",
                    (String) null
            );

            JsonNode properties = response.path(indexName).path("mappings").path("properties");
            if (properties.isMissingNode()) {
                return Collections.emptyList();
            }

            return findTextFields(properties, "");
        } catch (ServiceException e) {
            logger.warn("Reading resourceType '{}' fields from Elastic failed, using 'searchableArea' and 'payload' instead.", indexName, e);
            return List.of("searchableArea", "payload");
        }
    }

    /**
     * Recursively traverses an Elasticsearch mapping tree and collects all text fields.
     */
    private List<String> findTextFields(JsonNode properties, String pathPrefix) {
        List<String> result = new ArrayList<>();

        properties.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldProps = entry.getValue();
            String fullPath = pathPrefix.isEmpty() ? fieldName : pathPrefix + "." + fieldName;

            if ("text".equals(fieldProps.path("type").asText())) {
                result.add(fullPath);
            }

            JsonNode nestedProperties = fieldProps.path("properties");
            if (!nestedProperties.isMissingNode()) {
                result.addAll(findTextFields(nestedProperties, fullPath));
            }

            JsonNode subfields = fieldProps.path("fields");
            if (!subfields.isMissingNode()) {
                subfields.fields().forEachRemaining(subfieldEntry -> {
                    if ("text".equals(subfieldEntry.getValue().path("type").asText())) {
                        result.add(fullPath + "." + subfieldEntry.getKey());
                    }
                });
            }
        });

        return result;
    }

    /**
     * Executes a grouped search and returns the top hits per aggregation bucket.
     */
    private Map<String, List<Resource>> buildTopHitAggregation(FacetFilter filter, String category) {
        ObjectNode body = baseSearchBody(filter, 0, false);
        ObjectNode termsAggregation = mapper.createObjectNode();
        termsAggregation.putObject("terms")
                .put("field", category)
                .put("size", bucketSize);
        ObjectNode topHits = termsAggregation.putObject("aggs").putObject("documents").putObject("top_hits");
        topHits.put("size", topHitsSize);
        topHits.set("_source", includesNode());

        body.putObject("aggs").set("agg_category", termsAggregation);

        JsonNode response = executeSearch(filter.getResourceType(), body);
        JsonNode buckets = response.path("aggregations").path("agg_category").path("buckets");
        Map<String, List<Resource>> results = new HashMap<>();
        for (JsonNode bucket : buckets) {
            results.put(bucket.path("key").asText(),
                    toResources(bucket.path("documents").path("hits").path("hits")));
        }
        return results;
    }

    /**
     * Executes a standard search with highlighting enabled.
     */
    private Paging<HighlightedResult<Resource>> buildSearchWithHighlights(FacetFilter filter) {
        filter.setBrowseBy(resolveBrowseBy(filter));
        int quantity = filter.getQuantity();
        validateQuantity(quantity);

        ObjectNode body = baseSearchBody(filter, quantity, true);
        ObjectNode highlight = body.putObject("highlight");
        highlight.put("order", "score");
        highlight.putObject("fields").putObject("*.analyzed")
                .put("fragment_size", 2000)
                .put("number_of_fragments", 5);

        JsonNode response = executeSearch(filter.getResourceType(), body);
        return highlightedResponseToPaging(response, filter.getFrom(), filter.getBrowseBy(), filter.getResourceType());
    }

    /**
     * Executes a standard faceted search without highlights.
     */
    private Paging<Resource> buildSearch(FacetFilter filter) {
        filter.setBrowseBy(resolveBrowseBy(filter));
        int quantity = filter.getQuantity();
        validateQuantity(quantity);

        JsonNode response = executeSearch(filter.getResourceType(), baseSearchBody(filter, quantity, true));
        return responseToPaging(response, filter.getFrom(), filter.getBrowseBy(), filter.getResourceType());
    }

    /**
     * Creates the common request body shared by search endpoints.
     */
    private ObjectNode baseSearchBody(FacetFilter filter, int quantity, boolean includeBrowseBy) {
        ObjectNode body = mapper.createObjectNode();
        body.set("query", createQueryNode(filter));
        body.set("_source", includesNode());
        body.put("from", filter.getFrom());
        body.put("size", quantity);
        body.put("track_total_hits", true);

        applySorting(filter.getOrderBy(), body);
        if (includeBrowseBy) {
            applyBrowseByAggregations(filter.getBrowseBy(), body);
        }
        return body;
    }

    /**
     * Renders sort directives from the registry search model into Elasticsearch JSON.
     */
    private void applySorting(Map<String, Object> orderBy, ObjectNode body) {
        if (orderBy == null || orderBy.isEmpty()) {
            return;
        }
        ArrayNode sortArray = body.putArray("sort");
        for (Map.Entry<String, Object> order : orderBy.entrySet()) {
            Map<?, ?> op = (Map<?, ?>) order.getValue();
            sortArray.addObject().putObject(order.getKey()).put("order", op.get("order").toString());
        }
    }

    /**
     * Appends terms aggregations for each requested browse-by field.
     */
    private void applyBrowseByAggregations(List<String> browseBy, ObjectNode body) {
        if (browseBy == null || browseBy.isEmpty()) {
            return;
        }
        ObjectNode aggs = body.with("aggs");
        for (String browseField : browseBy) {
            aggs.putObject("by_" + browseField).putObject("terms")
                    .put("field", browseField)
                    .put("size", bucketSize);
        }
    }

    /**
     * Converts an Elasticsearch terms aggregation into the registry {@link Facet} model.
     */
    private Facet transformAggregation(String browseBy, JsonNode buckets, Map<String, String> fieldLabels) {
        Facet facet = new Facet();
        facet.setField(browseBy);
        facet.setLabel(fieldLabels.get(browseBy));
        List<gr.uoa.di.madgik.registry.domain.Value> values = new ArrayList<>();
        if (buckets != null && buckets.isArray()) {
            buckets.forEach(bucket -> values.add(new gr.uoa.di.madgik.registry.domain.Value(
                    bucket.path("key").asText(),
                    bucket.path("doc_count").asLong()
            )));
            Collections.sort(values);
        }
        facet.setValues(values);
        return facet;
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
        ElasticsearchQueryGenerator generator;
        try {
            generator = new ElasticsearchQueryGenerator(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        parser.getCQLQuery().accept(generator);

        ObjectNode body = mapper.createObjectNode();
        try {
            body.set("query", mapper.readTree(generator.getQueryResult()));
        } catch (IOException e) {
            throw new ServiceException("Failed to parse generated CQL query", e);
        }
        body.set("_source", includesNode());
        body.put("size", quantity);
        body.put("from", from);
        body.put("track_total_hits", true);

        if (!sortByField.isEmpty()) {
            body.putArray("sort").addObject().putObject(sortByField).put("order", sortOrder);
        }

        JsonNode response = executeSearch(resourceType, body);
        return responseToPaging(response, from, null, null);
    }

    private Paging<Resource> responseToPaging(JsonNode response, int from, List<String> browseBy,
                                              String resourceTypeName) {
        JsonNode hits = response.path("hits").path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            return new Paging<>();
        }

        List<Resource> resources = toResources(hits);

        List<Facet> facets = new ArrayList<>();
        if (browseBy != null) {
            Map<String, String> fieldLabels = resourceTypeService.getIndexFieldLabels(resourceTypeName);
            facets = browseBy.stream()
                    .map(x -> transformAggregation(x, response.path("aggregations").path("by_" + x).path("buckets"), fieldLabels))
                    .collect(Collectors.toList());
        }

        return new Paging<>(extractTotal(response), from, from + resources.size(), resources, facets);
    }

    /**
     * Converts a highlighted Elasticsearch response into the registry paging model.
     */
    private Paging<HighlightedResult<Resource>> highlightedResponseToPaging(JsonNode response, int from,
                                                                            List<String> browseBy,
                                                                            String resourceTypeName) {
        JsonNode hits = response.path("hits").path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            return new Paging<>();
        }

        List<HighlightedResult<Resource>> resources = new ArrayList<>();
        for (JsonNode hit : hits) {
            Resource resource = toResource(hit);
            HighlightedResult<Resource> result = new HighlightedResult<>();
            result.setHighlights(getHighlightsFromMap(hit.path("highlight")));
            result.setResult(resource);
            result.setScore((float) hit.path("_score").asDouble(0.0));
            resources.add(result);
        }

        List<Facet> facets = new ArrayList<>();
        if (browseBy != null) {
            Map<String, String> fieldLabels = resourceTypeService.getIndexFieldLabels(resourceTypeName);
            facets = browseBy.stream()
                    .map(x -> transformAggregation(x, response.path("aggregations").path("by_" + x).path("buckets"), fieldLabels))
                    .toList();
        }

        return new Paging<>(extractTotal(response), from, from + resources.size(), resources, facets);
    }

    /**
     * Resolves browse-by fields for direct resource types and alias groups.
     */
    private List<String> resolveBrowseBy(FacetFilter filter) {
        ResourceType rt = resourceTypeService.getResourceType(filter.getResourceType());
        List<ResourceType> resourceTypes = rt != null
                ? List.of(rt)
                : resourceTypeService.getAllResourceTypeByAlias(filter.getResourceType());
        return SearchService.resolveBrowseBy(resourceTypes, filter.getBrowseBy());
    }

    /**
     * Converts Elasticsearch highlight fragments into the registry highlight model.
     */
    private List<Highlight> getHighlightsFromMap(JsonNode highlightsMap) {
        List<Highlight> highlights = new ArrayList<>();
        if (!highlightsMap.isObject()) {
            return highlights;
        }
        highlightsMap.fields().forEachRemaining(hf -> {
            String key = hf.getKey().replace(".analyzed", "");
            hf.getValue().forEach(highlight -> highlights.add(new Highlight(key, highlight.asText())));
        });
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

        ObjectNode bool = mapper.createObjectNode();
        ArrayNode mustNot = bool.putArray("must_not");
        mustNot.addObject().putObject("terms")
                .set(resourceIdAndValue.getField(), mapper.createArrayNode().add(resourceIdAndValue.getValue()));

        ArrayNode must = bool.putArray("must");
        ObjectNode scriptScore = must.addObject().putObject("script_score");
        scriptScore.putObject("query").putObject("match_all");
        scriptScore.set("script", cosineScriptScoreQuery(embedding));

        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            Set<String> textFields = new HashSet<>(getTextFields(filter.getResourceType()));
            ObjectNode multiMatch = must.addObject().putObject("multi_match");
            multiMatch.put("query", filter.getKeyword());
            ArrayNode fields = multiMatch.putArray("fields");
            textFields.forEach(fields::add);
        }

        applyFilters(filter.getFilter(), bool);

        ObjectNode body = mapper.createObjectNode();
        body.set("query", mapper.createObjectNode().set("bool", bool));
        body.set("_source", includesNode());
        body.put("from", filter.getFrom());
        body.put("size", quantity);
        body.put("track_total_hits", true);

        JsonNode response = executeSearch(filter.getResourceType(), body);
        return toResources(response.path("hits").path("hits"));
    }

    /**
     * Loads the stored embedding of the reference resource used by recommendation queries.
     */
    private float[] getEmbeddingForResource(String resourceType, KeyValue resourceIdAndValue) {
        ObjectNode body = mapper.createObjectNode();
        body.put("size", 1);
        body.put("track_total_hits", true);
        body.putArray("_source").add("embedding");
        body.set("query", termsQuery(resourceIdAndValue.getField(), List.of(resourceIdAndValue.getValue())));

        JsonNode response = executeSearch(resourceType, body);
        JsonNode hits = response.path("hits").path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            throw new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException("There are no recommendations available for this resource",
                    new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException("Could not find resource"));
        }

        JsonNode embeddingNode = hits.get(0).path("_source").path("embedding");
        if (!embeddingNode.isArray()) {
            throw new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException("There are no recommendations available for this resource",
                    new gr.uoa.di.madgik.registry.exception.ResourceNotFoundException("Embedding field missing"));
        }
        return mapper.convertValue(embeddingNode, float[].class);
    }

    /**
     * Returns whether an embedding vector is absent or effectively empty.
     */
    private boolean embeddingIsEmpty(float[] embedding) {
        boolean empty = true;
        if (embedding != null) {
            for (float x : embedding) {
                if (x != 0 && !Float.isNaN(x)) {
                    empty = false;
                    break;
                }
            }
        }
        return empty;
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
        logger.debug(String.format("@Retryable 'searchId(resourceType=%s, ids={%s})'", resourceType, String.join(",", java.util.Arrays.stream(fields).map(keyValue -> keyValue.getField() + "=" + keyValue.getValue()).collect(Collectors.toSet()))));

        ObjectNode bool = mapper.createObjectNode();
        ArrayNode must = bool.putArray("must");
        java.util.Arrays.stream(fields)
                .forEach(kv -> must.add(termsQuery(kv.getField(), List.of(kv.getValue()))));

        ObjectNode body = mapper.createObjectNode();
        body.set("query", mapper.createObjectNode().set("bool", bool));
        body.set("_source", includesNode());
        body.put("size", 1);
        body.put("track_total_hits", true);

        JsonNode response = executeSearch(resourceType, body);
        JsonNode hits = response.path("hits").path("hits");
        return hits.isArray() && !hits.isEmpty() ? toResource(hits.get(0)) : null;
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

        ObjectNode body = mapper.createObjectNode();
        body.set("query", termsQuery(idField, ids));
        body.put("_source", false);
        ArrayNode fields = body.putArray("fields");
        fields.add(idField);
        fields.add(labelField);
        body.put("size", ids.size());
        body.put("track_total_hits", true);

        logger.debug("getLabels: index='{}', idField='{}', labelField='{}', ids={}",
                resourceType, idField, labelField, ids);

        JsonNode response = executeSearch(resourceType, body);
        int total = extractTotal(response);
        logger.debug("getLabels: total hits={}", total);

        Map<String, String> result = new HashMap<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode hitFields = hit.path("fields");
            JsonNode idValues = hitFields.path(idField);
            JsonNode labelValues = hitFields.path(labelField);
            if (!idValues.isMissingNode() && !labelValues.isMissingNode()
                    && idValues.isArray() && labelValues.isArray()
                    && !idValues.isEmpty() && !labelValues.isEmpty()) {
                result.put(idValues.get(0).asText(), labelValues.get(0).asText());
            }
        }
        logger.debug("getLabels: resolved {}/{} labels", result.size(), ids.size());
        return result;
    }

    private void validateQuantity(int quantity) {
        if (quantity > maxQuantity) {
            throw new IllegalArgumentException(String.format("Quantity should be up to %s.", maxQuantity));
        } else if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    /**
     * Executes the rendered Elasticsearch search request against the target index.
     */
    private JsonNode executeSearch(String resourceType, ObjectNode body) {
        try {
            return ElasticRestUtils.performJsonRequest(
                    elasticsearchClient,
                    mapper,
                    "POST",
                    "/" + resourceType + "/_search",
                    Map.of("search_type", "dfs_query_then_fetch"),
                    mapper.writeValueAsString(body)
            );
        } catch (IOException e) {
            throw new ServiceException("Failed to serialize Elasticsearch query", e);
        }
    }

    /**
     * Builds the source filtering array used by registry search responses.
     */
    private ArrayNode includesNode() {
        ArrayNode includes = mapper.createArrayNode();
        for (String include : INCLUDES) {
            includes.add(include);
        }
        return includes;
    }

    /**
     * Extracts total hits from both legacy integer and object-based Elasticsearch formats.
     */
    private int extractTotal(JsonNode response) {
        JsonNode total = response.path("hits").path("total");
        if (total.isIntegralNumber()) {
            return total.asInt();
        }
        return total.path("value").asInt(0);
    }

    /**
     * Deserializes a hit array into registry resources.
     */
    private List<Resource> toResources(JsonNode hits) {
        List<Resource> resources = new ArrayList<>();
        if (hits == null || !hits.isArray()) {
            return resources;
        }
        for (JsonNode hit : hits) {
            resources.add(toResource(hit));
        }
        return resources;
    }

    /**
     * Deserializes a single Elasticsearch hit into a registry resource.
     */
    private Resource toResource(JsonNode hit) {
        try {
            Resource resource = mapper.treeToValue(hit.path("_source"), Resource.class);
            resource.setResourceTypeName(hit.path("_index").asText());
            return resource;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * Builds a JSON {@code terms} query for the provided field and values.
     */
    private ObjectNode termsQuery(String field, List<String> values) {
        ObjectNode terms = mapper.createObjectNode();
        ArrayNode array = mapper.createArrayNode();
        values.forEach(array::add);
        terms.putObject("terms").set(field, array);
        return terms;
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
