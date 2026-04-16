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

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.elasticsearch.autoconfigure.RegistryElasticsearchProperties;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Elasticsearch range query JSON structure produced by
 * {@link ElasticSearchService} when {@link FacetFilter#addRangeFilter} is used.
 *
 * <p>Uses reflection to invoke the private {@code createQueryNode} method so the
 * query JSON can be inspected without a running Elasticsearch instance.</p>
 */
class ElasticSearchServiceRangeQueryTest {

    private ElasticSearchService service;
    private Method createQueryNode;
    private Method createHybridQueryNode;
    private Method createSemanticQueryNode;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private EmbeddingService embeddingService;
    private co.elastic.clients.elasticsearch.ElasticsearchClient client;

    @BeforeEach
    void setUp() throws Exception {
        embeddingService = mock(EmbeddingService.class);
        client = mock(co.elastic.clients.elasticsearch.ElasticsearchClient.class);
        co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient indicesClient =
                mock(co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient.class);
        when(client.indices()).thenReturn(indicesClient);
        doThrow(new IOException("mapping unavailable"))
                .when(indicesClient)
                .getMapping(org.mockito.ArgumentMatchers.any(java.util.function.Function.class));

        RegistryElasticsearchProperties elasticsearchProperties = new RegistryElasticsearchProperties();
        service = new ElasticSearchService(
                client,
                mock(JacksonJsonpMapper.class),
                embeddingService,
                mock(ResourceTypeService.class),
                elasticsearchProperties
        );
        // Allow access to private createQueryNode(FacetFilter)
        createQueryNode = ElasticSearchService.class.getDeclaredMethod("createQueryNode", FacetFilter.class);
        createQueryNode.setAccessible(true);
        createHybridQueryNode = ElasticSearchService.class.getDeclaredMethod("createHybridQueryNode", FacetFilter.class);
        createHybridQueryNode.setAccessible(true);
        createSemanticQueryNode = ElasticSearchService.class.getDeclaredMethod("createSemanticQueryNode", FacetFilter.class);
        createSemanticQueryNode.setAccessible(true);
    }

    // -------------------------------------------------------------------------
    // No range filters — baseline
    // -------------------------------------------------------------------------

    @Test
    void noRangeFilters_queryContainsMatchAll() throws Exception {
        FacetFilter filter = filter("my_index");

        ObjectNode query = invoke(filter);
        ObjectNode bool = (ObjectNode) query.get("bool");

        assertNotNull(bool);
        ArrayNode must = (ArrayNode) bool.get("must");
        assertEquals(1, must.size(), "Only match_all expected when no filters are set");
        assertNotNull(must.get(0).get("match_all"));
    }

    // -------------------------------------------------------------------------
    // includeNull = false — plain range query
    // -------------------------------------------------------------------------

    @Test
    void rangeFilter_noIncludeNull_addsRangeClause() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("age", 20L, 30L, false);

        ObjectNode query = invoke(filter);
        ArrayNode must = must(query);

        assertEquals(2, must.size());
        ObjectNode rangeNode = (ObjectNode) must.get(1);
        ObjectNode ageRange = (ObjectNode) rangeNode.get("range").get("age");
        assertEquals(20, ageRange.get("gte").asInt());
        assertEquals(30, ageRange.get("lte").asInt());
    }

    @Test
    void rangeFilter_onlyLowerBound_noLteInQuery() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("age", 20L, null, false);

        ObjectNode ageRange = rangeClause(invoke(filter), "age");

        assertNotNull(ageRange.get("gte"));
        assertNull(ageRange.get("lte"));
    }

    @Test
    void rangeFilter_onlyUpperBound_noGteInQuery() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("age", null, 30L, false);

        ObjectNode ageRange = rangeClause(invoke(filter), "age");

        assertNull(ageRange.get("gte"));
        assertNotNull(ageRange.get("lte"));
    }

    // -------------------------------------------------------------------------
    // includeNull = true — wraps range in bool/should with missing-field clause
    // -------------------------------------------------------------------------

    @Test
    void rangeFilter_includeNull_wrapsBoolShould() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("expiryDate", new Date(1000L), null, true);

        ObjectNode query = invoke(filter);
        ArrayNode must = must(query);

        assertEquals(2, must.size());
        // The range clause should be wrapped in a bool/should
        ObjectNode wrapper = (ObjectNode) must.get(1);
        ObjectNode innerBool = (ObjectNode) wrapper.get("bool");
        assertNotNull(innerBool, "Expected a bool wrapper for includeNull");
        assertEquals(1, innerBool.get("minimum_should_match").asInt());

        ArrayNode should = (ArrayNode) innerBool.get("should");
        assertEquals(2, should.size());

        // First should: must_not exists (missing field → passes)
        ObjectNode missingClause = (ObjectNode) should.get(0).get("bool").get("must_not").get("exists");
        assertEquals("expiryDate", missingClause.get("field").asText());

        // Second should: range
        assertNotNull(should.get(1).get("range").get("expiryDate"));
    }

    @Test
    void rangeFilter_includeNull_bothBounds() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("publishDate", new Date(1000L), new Date(5000L), true);

        ObjectNode query = invoke(filter);
        ArrayNode should = must(query).get(1).get("bool").withArray("should");

        // range clause has both gte and lte
        ObjectNode dateRange = (ObjectNode) should.get(1).get("range").get("publishDate");
        assertNotNull(dateRange.get("gte"));
        assertNotNull(dateRange.get("lte"));
    }

    // -------------------------------------------------------------------------
    // Both bounds null
    // -------------------------------------------------------------------------

    @Test
    void bothBoundsNull_includeNull_true_emitsMustNotExists() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("expiryDate", null, null, true);

        ObjectNode query = invoke(filter);
        ArrayNode must = must(query);

        // match_all + must_not_exists wrapper (no range clause)
        assertEquals(2, must.size());
        ObjectNode innerBool = (ObjectNode) must.get(1).get("bool");
        assertNotNull(innerBool, "Expected bool/must_not clause");
        ObjectNode existsClause = (ObjectNode) innerBool.get("must_not").get("exists");
        assertEquals("expiryDate", existsClause.get("field").asText());
        // No range clause present
        assertNull(must.get(1).get("range"));
    }

    @Test
    void bothBoundsNull_includeNull_false_addsNoClause() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("expiryDate", null, null, false);

        ObjectNode query = invoke(filter);
        ArrayNode must = must(query);

        // Only match_all — no extra clause added
        assertEquals(1, must.size());
    }

    // -------------------------------------------------------------------------
    // Multiple range filters
    // -------------------------------------------------------------------------

    @Test
    void multipleRangeFilters_eachProducesOwnMustClause() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addRangeFilter("publishDate", null, new Date(), true);
        filter.addRangeFilter("expiryDate", new Date(), null, true);

        ObjectNode query = invoke(filter);
        ArrayNode must = must(query);

        // match_all + 2 range wrappers
        assertEquals(3, must.size());
    }

    // -------------------------------------------------------------------------
    // Combined equality + range
    // -------------------------------------------------------------------------

    @Test
    void equalityAndRangeFilter_bothApplied() throws Exception {
        FacetFilter filter = filter("my_index");
        filter.addFilter("status", "APPROVED");
        filter.addRangeFilter("age", 20L, 30L, false);

        ObjectNode query = invoke(filter);
        ArrayNode must = must(query);

        // match_all + term(status) + range(age)
        assertEquals(3, must.size());
        assertNotNull(must.get(1).get("term"), "Expected term filter for status");
        assertNotNull(must.get(2).get("range"), "Expected range filter for age");
    }

    @Test
    void hybridKeywordQuery_usesMultiMatchAndKnnInsteadOfScriptScore() throws Exception {
        when(embeddingService.embed("registry")).thenReturn(new float[]{0.2f, 0.4f});

        FacetFilter filter = filter("my_index");
        filter.setKeyword("registry");
        filter.setQuantity(10);

        ObjectNode query = invokeHybrid(filter);
        ObjectNode bool = (ObjectNode) query.get("bool");

        assertEquals(1, bool.get("minimum_should_match").asInt());
        ArrayNode should = bool.withArray("should");
        assertEquals(2, should.size());
        assertNotNull(should.get(0).get("multi_match"));
        assertNotNull(should.get(1).get("knn"));
        ArrayNode must = must(query);
        for (int i = 0; i < must.size(); i++) {
            assertNull(must.get(i).get("script_score"));
        }
    }

    @Test
    void hybridKeywordQuery_skipsKnnWhenEmbeddingIsEmpty() throws Exception {
        when(embeddingService.embed("registry")).thenReturn(new float[]{0.0f, Float.NaN});

        FacetFilter filter = filter("my_index");
        filter.setKeyword("registry");

        ObjectNode query = invokeHybrid(filter);
        ArrayNode should = query.get("bool").withArray("should");

        assertEquals(1, should.size());
        assertNotNull(should.get(0).get("multi_match"));
    }

    @Test
    void semanticQuery_containsOnlyKnnAndFilters() throws Exception {
        when(embeddingService.embed("registry")).thenReturn(new float[]{0.2f, 0.4f});

        FacetFilter filter = filter("my_index");
        filter.setKeyword("registry");
        filter.addFilter("status", "APPROVED");

        ObjectNode query = invokeSemantic(filter);
        ArrayNode must = must(query);

        assertNotNull(must.get(0).get("knn"));
        assertNotNull(must.get(1).get("term"));
        assertFalse(query.get("bool").has("should"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FacetFilter filter(String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType);
        return ff;
    }

    private ObjectNode invoke(FacetFilter filter) throws Exception {
        return (ObjectNode) createQueryNode.invoke(service, filter);
    }

    private ObjectNode invokeHybrid(FacetFilter filter) throws Exception {
        return (ObjectNode) createHybridQueryNode.invoke(service, filter);
    }

    private ObjectNode invokeSemantic(FacetFilter filter) throws Exception {
        return (ObjectNode) createSemanticQueryNode.invoke(service, filter);
    }

    private ArrayNode must(ObjectNode query) {
        return (ArrayNode) query.get("bool").get("must");
    }

    /**
     * Drills into the must array and finds the plain {@code range} clause for the given field.
     * Assumes includeNull=false (no bool/should wrapper).
     */
    private ObjectNode rangeClause(ObjectNode query, String field) {
        ArrayNode must = must(query);
        for (int i = 0; i < must.size(); i++) {
            ObjectNode rangeNode = (ObjectNode) must.get(i).get("range");
            if (rangeNode != null && rangeNode.has(field)) {
                return (ObjectNode) rangeNode.get(field);
            }
        }
        fail("No range clause found for field: " + field);
        return null;
    }
}
