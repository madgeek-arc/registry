/*
 * Copyright 2026-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class DefaultSearchServiceSemanticTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingService embeddingService;

    @MockitoBean
    AuditActorProvider auditActorProvider;

    @Autowired
    SearchService searchService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    ViewService viewService;

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceChunkIndexService resourceChunkIndexService;

    @BeforeAll
    void createEmployeeView() {
        viewService.createView(resourceTypeService.getResourceType("employee"));
    }

    @BeforeEach
    void stubEmbeddingService() {
        when(auditActorProvider.currentActor()).thenReturn("semantic-test");
        lenient().when(embeddingService.embed(anyString())).thenAnswer(invocation ->
                embeddingFor(invocation.getArgument(0, String.class)));
        lenient().when(embeddingService.modelName()).thenReturn("test-embedding-model");
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_emptyKeyword_throwsServiceException() {
        FacetFilter filter = employeeFilter();
        // No keyword set — semantic search must reject this
        ServiceException ex = assertThrows(ServiceException.class,
                () -> searchService.semanticSearch(filter));
        assertEquals("Semantic search requires a non-empty keyword.", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // No-chunks / fallback behaviour
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_noChunks_returnsEmptyPaging() {
        // Return a bad embedding during addResource so the auto-reindex stores no chunks.
        when(embeddingService.embed(anyString())).thenReturn(new float[0]);
        resourceService.addResource(newEmployeeResource("Analytics Director", 45));
        // Restore real embeddings for the search query.
        when(embeddingService.embed(anyString())).thenAnswer(invocation ->
                embeddingFor(invocation.getArgument(0, String.class)));

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");

        Paging<Resource> results = searchService.semanticSearch(filter);

        assertEquals(0, results.getTotal());
        assertTrue(results.getResults().isEmpty());
    }

    @Test
    void hybridSearch_emptyKeyword_fallsBackToLexicalSearch() {
        // With no keyword, hybridSearch delegates to search().
        // The fixture employee ("Jodeee") should be returned by the plain lexical path.
        FacetFilter hybridFilter = employeeFilter();
        FacetFilter lexicalFilter = employeeFilter();

        Paging<Resource> hybrid = searchService.hybridSearch(hybridFilter);
        Paging<Resource> lexical = searchService.search(lexicalFilter);

        assertEquals(lexical.getTotal(), hybrid.getTotal());
        assertEquals(
                lexical.getResults().stream().map(Resource::getId).toList(),
                hybrid.getResults().stream().map(Resource::getId).toList()
        );
    }

    @Test
    void semanticSearch_withNullEmbedding_returnsEmptyPaging() {
        Resource analytics = resourceService.addResource(newEmployeeResource("Analytics Manager", 38));
        resourceChunkIndexService.reindex(analytics);

        // Override stub to return null — isUsableEmbedding() rejects it.
        when(embeddingService.embed(anyString())).thenReturn(null);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");

        Paging<Resource> results = searchService.semanticSearch(filter);

        assertEquals(0, results.getTotal());
        assertTrue(results.getResults().isEmpty());
    }

    @Test
    void hybridSearch_withNullEmbedding_fallsBackToLexicalSearch() {
        // Hybrid falls back to plain search() when the embedding is unusable.
        // The fixture employee ("Jodeee") must be returned via the lexical path.
        when(embeddingService.embed(anyString())).thenReturn(null);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("Jodeee");

        Paging<Resource> results = searchService.hybridSearch(filter);

        assertTrue(results.getTotal() >= 1);
        assertTrue(results.getResults().stream()
                .anyMatch(r -> DatabaseConfiguration.TEST_RESOURCE_ID.equals(r.getId())));
    }

    // -------------------------------------------------------------------------
    // Filter interaction
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_withEqualityFilter_narrowsCandidatePool() {
        Resource young = resourceService.addResource(newEmployeeResource("Analytics Lead", 34));
        Resource senior = resourceService.addResource(newEmployeeResource("Analytics Expert", 50));
        resourceChunkIndexService.reindex(young);
        resourceChunkIndexService.reindex(senior);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");
        filter.addFilter("age", 34);

        Paging<Resource> results = searchService.semanticSearch(filter);

        assertEquals(1, results.getTotal());
        assertEquals(young.getId(), results.getResults().getFirst().getId());
    }

    @Test
    void hybridSearch_withRangeFilter_narrowsResults() {
        Resource young = resourceService.addResource(newEmployeeResource("Analytics Young", 25));
        Resource senior = resourceService.addResource(newEmployeeResource("Analytics Senior", 55));
        resourceChunkIndexService.reindex(young);
        resourceChunkIndexService.reindex(senior);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");
        filter.addRangeFilter("age", 20, 40, false);

        Paging<Resource> results = searchService.hybridSearch(filter);

        assertEquals(1, results.getTotal());
        assertEquals(young.getId(), results.getResults().getFirst().getId());
    }

    // -------------------------------------------------------------------------
    // Facets
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_withBrowseBy_populatesFacets() {
        Resource r34 = resourceService.addResource(newEmployeeResource("Analytics Engineer", 34));
        Resource r50 = resourceService.addResource(newEmployeeResource("Analytics Manager", 50));
        resourceChunkIndexService.reindex(r34);
        resourceChunkIndexService.reindex(r50);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");
        filter.setBrowseBy(List.of("age"));

        Paging<Resource> results = searchService.semanticSearch(filter);

        assertTrue(results.getTotal() >= 2);
        assertFalse(results.getFacets().isEmpty());
        Facet ageFacet = results.getFacets().stream()
                .filter(f -> "age".equals(f.getField()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected 'age' facet"));
        assertTrue(ageFacet.getValues().size() >= 2);
    }

    @Test
    void semanticSearch_filtersOutHitsBelowConfiguredMinimumScore() {
        Resource analytics = resourceService.addResource(newEmployeeResource("Analytics Engineer", 34));
        resourceChunkIndexService.reindex(analytics);

        when(embeddingService.embed("weak analytics")).thenReturn(vector(0.2f, 0.9797959f, 0f));

        FacetFilter filter = employeeFilter();
        filter.setKeyword("weak analytics");

        Paging<Resource> results = searchService.semanticSearch(filter);

        assertEquals(0, results.getTotal());
        assertTrue(results.getResults().isEmpty());
    }

    @Test
    void hybridSearch_withBrowseBy_populatesFacets() {
        Resource r34 = resourceService.addResource(newEmployeeResource("Analytics Analyst", 34));
        Resource r50 = resourceService.addResource(newEmployeeResource("Analytics Consultant", 50));
        resourceChunkIndexService.reindex(r34);
        resourceChunkIndexService.reindex(r50);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");
        filter.setBrowseBy(List.of("age"));

        Paging<Resource> results = searchService.hybridSearch(filter);

        assertTrue(results.getTotal() >= 2);
        Facet ageFacet = results.getFacets().stream()
                .filter(f -> "age".equals(f.getField()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected 'age' facet"));
        assertTrue(ageFacet.getValues().size() >= 2);
    }

    // -------------------------------------------------------------------------
    // Hybrid FULL OUTER JOIN — both sides contribute
    // -------------------------------------------------------------------------

    @Test
    void hybridSearch_includesBothLexicalOnlyAndSemanticOnlyHits() {
        // "Analytics Docs" matches both lexically (payload contains "analytics") and semantically.
        // "Data Scientist" is semantic-only: its embedding (0.8,0.2,0) is close to the "analytics"
        // query vector (1,0,0) but its payload does not contain the word "analytics".
        Resource lexicalAndSemantic = resourceService.addResource(
                newEmployeeResource("Analytics Docs", 30));
        Resource semanticOnly = resourceService.addResource(
                newEmployeeResource("Data Scientist", 30));
        resourceChunkIndexService.reindex(lexicalAndSemantic);
        resourceChunkIndexService.reindex(semanticOnly);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");

        Paging<Resource> results = searchService.hybridSearch(filter);

        List<String> ids = results.getResults().stream().map(Resource::getId).toList();
        assertTrue(ids.contains(lexicalAndSemantic.getId()),
                "Expected lexical+semantic hit to be present");
        assertTrue(ids.contains(semanticOnly.getId()),
                "Expected semantic-only hit to be present via FULL OUTER JOIN");
        // The resource matching on both sides must rank above the semantic-only one.
        assertTrue(ids.indexOf(lexicalAndSemantic.getId()) < ids.indexOf(semanticOnly.getId()),
                "Lexical+semantic match must rank above semantic-only match");
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_paginatesResults() {
        Resource alpha = resourceService.addResource(newEmployeeResource("Analytics Alpha", 30));
        Resource beta = resourceService.addResource(newEmployeeResource("Analytics Beta", 31));
        Resource gamma = resourceService.addResource(newEmployeeResource("Analytics Gamma", 32));
        resourceChunkIndexService.reindex(alpha);
        resourceChunkIndexService.reindex(beta);
        resourceChunkIndexService.reindex(gamma);

        FacetFilter firstPage = employeeFilter();
        firstPage.setKeyword("analytics");
        firstPage.setFrom(0);
        firstPage.setQuantity(2);

        Paging<Resource> page1 = searchService.semanticSearch(firstPage);

        assertEquals(3, page1.getTotal());
        assertEquals(2, page1.getResults().size());

        FacetFilter secondPage = employeeFilter();
        secondPage.setKeyword("analytics");
        secondPage.setFrom(2);
        secondPage.setQuantity(2);

        Paging<Resource> page2 = searchService.semanticSearch(secondPage);

        assertEquals(3, page2.getTotal());
        assertEquals(1, page2.getResults().size());
        // No overlap between pages
        List<String> page1Ids = page1.getResults().stream().map(Resource::getId).toList();
        List<String> page2Ids = page2.getResults().stream().map(Resource::getId).toList();
        assertTrue(page2Ids.stream().noneMatch(page1Ids::contains));
    }

    // -------------------------------------------------------------------------
    // Highlights
    // -------------------------------------------------------------------------

    @Test
    void hybridSearchWithHighlights_returnsLexicalAndSemanticHighlights() {
        Resource resource = resourceService.addResource(newEmployeeResource("Analytics Hybrid", 36));
        resourceChunkIndexService.reindex(resource);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");

        Paging<HighlightedResult<Resource>> results = searchService.hybridSearchWithHighlights(filter);

        assertFalse(results.getResults().isEmpty());
        HighlightedResult<Resource> hit = results.getResults().stream()
                .filter(r -> resource.getId().equals(r.getResult().getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected the analytics resource in results"));

        // Indexed-field highlight: keyword in some indexed field wrapped in <em>
        assertTrue(hit.getHighlights().stream()
                .anyMatch(h -> !"payload".equals(h.getField())
                        && h.getValue().contains("<em>")),
                "Expected a lexical indexed-field highlight");

        assertFalse(hit.getHighlights().isEmpty(),
                "Expected at least one deduplicated highlight");
    }

    // -------------------------------------------------------------------------
    // Recommendations
    // -------------------------------------------------------------------------

    @Test
    void recommend_withKeywordFilter_narrowsCandidatePool() {
        // Source and the analytics peer both contain "analytics" in their payloads.
        // The operations peer does not — the keyword filter must exclude it.
        Resource source = resourceService.addResource(newEmployeeResource("Analytics Source", 28));
        Resource analyticsPeer = resourceService.addResource(newEmployeeResource("Analytics Peer", 29));
        Resource operationsPeer = resourceService.addResource(newEmployeeResource("Operations Peer", 29));
        resourceChunkIndexService.reindex(source);
        resourceChunkIndexService.reindex(analyticsPeer);
        resourceChunkIndexService.reindex(operationsPeer);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");

        List<Resource> recommendations = searchService.recommend(filter,
                new SearchService.KeyValue("resource_internal_id", source.getId()));

        List<String> ids = recommendations.stream().map(Resource::getId).toList();
        assertTrue(ids.contains(analyticsPeer.getId()),
                "Analytics peer must be recommended");
        assertFalse(ids.contains(operationsPeer.getId()),
                "Operations peer must be excluded by keyword filter");
        assertFalse(ids.contains(source.getId()),
                "Source resource must never appear in its own recommendations");
    }

    @Test
    void recommend_noChunksForSource_throwsResourceNotFoundException() {
        // Return a bad embedding during addResource so the auto-reindex stores no chunks.
        when(embeddingService.embed(anyString())).thenReturn(new float[0]);
        Resource source = resourceService.addResource(newEmployeeResource("Orphan Source", 40));
        // Restore real embeddings for subsequent calls.
        when(embeddingService.embed(anyString())).thenAnswer(invocation ->
                embeddingFor(invocation.getArgument(0, String.class)));

        FacetFilter filter = employeeFilter();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> searchService.recommend(filter,
                        new SearchService.KeyValue("resource_internal_id", source.getId())));
        assertTrue(ex.getMessage().contains("There are no recommendations available"),
                "Expected 'no recommendations' message, got: " + ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Alias support
    // -------------------------------------------------------------------------

    @Test
    void semanticSearch_usingAlias_returnsMatchingResources() {
        Resource analytics = resourceService.addResource(newEmployeeResource("Analytics Aliased", 33));
        resourceChunkIndexService.reindex(analytics);

        // "resourceTypes" is an alias for the "employee" resource type.
        FacetFilter filter = new FacetFilter();
        filter.setResourceType("resourceTypes");
        filter.setKeyword("analytics");
        filter.setQuantity(10);

        Paging<Resource> results = searchService.semanticSearch(filter);

        assertTrue(results.getTotal() >= 1);
        assertTrue(results.getResults().stream()
                .anyMatch(r -> analytics.getId().equals(r.getId())));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FacetFilter employeeFilter() {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType("employee");
        filter.setQuantity(10);
        return filter;
    }

    private Resource newEmployeeResource(String author, int age) {
        Resource resource = new Resource();
        resource.setResourceTypeName("employee");
        resource.setPayloadFormat("xml");
        resource.setPayload(newEmployeePayload(author, age));
        return resource;
    }

    private String newEmployeePayload(String author, int age) {
        return """
                <?xml version="1.0"?>
                <employee>
                  <author>%s</author>
                  <age>%d</age>
                  <single>false</single>
                  <birthday>645544821000</birthday>
                  <salary>1292.123</salary>
                  <amka>%d</amka>
                </employee>
                """.formatted(author, age, Math.abs(author.hashCode()) + 10000000000000L);
    }

    /**
     * Deterministic embedding function: keywords control which dimension is dominant,
     * making cosine-similarity ordering fully predictable in assertions.
     *
     * <ul>
     *   <li>"analytics" / "architect" → (1, 0, 0, …)</li>
     *   <li>"operations"              → (0, 1, 0, …)</li>
     *   <li>"jodeee"                  → (0, 0, 1, …)</li>
     *   <li>"data"                    → (0.8, 0.2, 0, …) — semantically close to analytics</li>
     *   <li>default                   → (0.2, 0.2, 0.2, …)</li>
     * </ul>
     */
    private float[] embeddingFor(String text) {
        String normalized = text == null ? "" : text.toLowerCase();
        if (normalized.contains("analytics") || normalized.contains("architect")) {
            return vector(1f, 0f, 0f);
        }
        if (normalized.contains("operations")) {
            return vector(0f, 1f, 0f);
        }
        if (normalized.contains("jodeee")) {
            return vector(0f, 0f, 1f);
        }
        if (normalized.contains("data")) {
            return vector(0.8f, 0.2f, 0f);
        }
        return vector(0.2f, 0.2f, 0.2f);
    }

    private float[] vector(float x, float y, float z) {
        float[] v = new float[EmbeddingService.VECTOR_SIZE];
        v[0] = x;
        v[1] = y;
        v[2] = z;
        return v;
    }
}
