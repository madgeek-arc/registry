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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.dao.ResourceChunkDao;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.registry.domain.ResourceChunk;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Value;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import gr.uoa.di.madgik.registry.exception.MissingResourceEmbeddingsException;
import gr.uoa.di.madgik.registry.exception.UnsupportedSearchParameterException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class DefaultSearchServiceQueryBuilderTest extends PostgreSqlTestContainerSupport {

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

    @Autowired
    ResourceChunkDao resourceChunkDao;

    @Autowired
    @Qualifier("registryDataSource")
    DataSource dataSource;

    @BeforeAll
    void createEmployeeView() {
        viewService.createView(resourceTypeService.getResourceType("employee"));
    }

    @BeforeEach
    void stubEmbeddingModel() {
        when(auditActorProvider.currentActor()).thenReturn("search-test");
        lenient().when(embeddingService.embed(anyString())).thenAnswer(invocation ->
                embeddingFor(invocation.getArgument(0, String.class)));
        lenient().when(embeddingService.modelName()).thenReturn("test-embedding-model");
    }

    @Test
    void cqlQuery_returnsMatchingResource() {
        Paging<Resource> result = searchService.cqlQuery("age = 28", "employee");

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void cqlQuery_supportsBooleanConnectors() {
        Paging<Resource> result = searchService.cqlQuery("age = 28 AND single = false", "employee");

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void cqlQuery_rejectsInjectedExpression() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> searchService.cqlQuery("age = 28; DROP TABLE resource", "employee"));

        assertEquals("Found terminating character ';' in cql query", exception.getMessage());
    }

    @Test
    void cqlQuery_rejectsUnknownFieldExpression() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> searchService.cqlQuery("unknown = 1 OR age = 28", "employee"));

        assertEquals("Unknown CQL field 'unknown' for resource type 'employee'", exception.getMessage());
    }

    @Test
    void cqlQuery_supportsOrExpressions() {
        Paging<Resource> result = searchService.cqlQuery("age = 99 OR single = false", "employee");

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void cqlQuery_supportsNotExpressions() {
        Paging<Resource> result = searchService.cqlQuery("age = 28 NOT single = true", "employee");

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void cqlQuery_supportsParenthesizedExpressions() {
        Paging<Resource> result = searchService.cqlQuery(
                "(age = 28 AND single = false) OR amka = 99999999999999", "employee");

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void cqlQuery_supportsInstantFloatAndLongCoercion() {
        Paging<Resource> result = searchService.cqlQuery(
                "birthday = \"1990-06-16T14:00:21Z\" AND salary = 1292.123 AND amka = 51417010293821",
                "employee");

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void cqlQuery_rejectsInvalidSyntax() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> searchService.cqlQuery("age =", "employee"));

        assertEquals("Invalid CQL query", exception.getMessage());
    }

    @Test
    void cqlQuery_rejectsInvalidInstantLiteral() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> searchService.cqlQuery("birthday = \"not-a-date\"", "employee"));

        assertEquals("Invalid instant value: not-a-date", exception.getMessage());
    }

    @Test
    void cqlQuery_rejectsUnsupportedSortByClause() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> searchService.cqlQuery("age = 28 sortBy age", "employee"));

        assertEquals("CQL sort clauses are not supported", exception.getMessage());
    }

    @Test
    void searchFields_returnsMatchingResource() {
        Resource resource = searchService.searchFields("employee",
                new SearchService.KeyValue("first_name", "Jodeee"));

        assertNotNull(resource);
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, resource.getId());
    }

    @Test
    void search_usingAlias_returnsMatchingResource() {
        FacetFilter filter = employeeFilter();
        filter.setResourceType("resourceTypes");

        Paging<Resource> result = searchService.search(filter);

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void search_doesNotMutateCallerKeyword() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("John");

        searchService.search(filter);

        assertEquals("John", filter.getKeyword());
    }

    @Test
    void search_populatesFacetValues() {
        FacetFilter filter = employeeFilter();
        filter.setBrowseBy(List.of("age"));

        Paging<Resource> result = searchService.search(filter);

        assertEquals(1, result.getFacets().size());
        Facet facet = result.getFacets().getFirst();
        assertEquals("age", facet.getField());
        assertEquals(1, facet.getValues().size());
        Value value = facet.getValues().getFirst();
        assertEquals("28", value.getValue());
        assertEquals(1L, value.getCount());
    }

    @Test
    void search_multivaluedField_usesArrayContainsPredicate() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("""
                INSERT INTO public.indexfield
                (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
                VALUES (?, NULL, ?, true, ?, false, ?, ?)
                """,
                "tags", "tags", "//*[local-name()='tag']/text()", "java.lang.String", "employee");
        jdbcTemplate.update("""
                INSERT INTO public.stringindexedfield (id, name, resource_id)
                VALUES (?, ?, ?)
                """,
                123129L, "tags", DatabaseConfiguration.TEST_RESOURCE_ID);
        jdbcTemplate.batchUpdate("""
                        INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
                        VALUES (?, ?)
                        """,
                List.of(new Object[]{123129L, "alpha"}, new Object[]{123129L, "beta"}));

        ResourceType employee = resourceTypeService.getResourceType("employee");
        viewService.createView(employee);

        FacetFilter filter = employeeFilter();
        filter.addFilter("tags", List.of("beta"));

        Paging<Resource> result = searchService.search(filter);

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void getLabels_rejectsUnknownResourceType() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> searchService.getLabels("employee; DROP VIEW employee_view", "first_name",
                        List.of("Jodeee"), "first_name"));

        assertEquals("Unknown resource type 'employee; DROP VIEW employee_view'", exception.getMessage());
    }

    @Test
    void getLabels_usesResolvedResourceTypeName() {
        Map<String, String> labels = searchService.getLabels("employee", "first_name",
                List.of("Jodeee"), "first_name");

        assertEquals(Map.of("Jodeee", "Jodeee"), labels);
    }

    @Test
    void searchWithHighlights_returnsHighlightFromTextOnlyField() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Add a TEXT-only 'bio' field to the employee type (mutates the shared in-memory cache)
        ResourceType employee = resourceTypeService.getResourceType("employee");
        List<IndexField> fields = employee.getIndexFields();
        fields.add(textOnlyIndexField("bio", employee));
        employee.setIndexFields(fields);

        // Store a bio value containing "Jodeee" so the payload-wide LIKE match fires on the
        // existing test resource and the bio field also gets a highlight
        jdbcTemplate.update("""
                INSERT INTO public.stringindexedfield (id, name, resource_id)
                VALUES (?, ?, ?)
                """, 551232L, "bio", DatabaseConfiguration.TEST_RESOURCE_ID);
        jdbcTemplate.update("""
                INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
                VALUES (?, ?)
                """, 551232L, "Jodeee loves data science");

        viewService.deleteView("employee");
        viewService.createView(employee);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("Jodeee");

        Paging<HighlightedResult<Resource>> result = searchService.searchWithHighlights(filter);

        assertFalse(result.getResults().isEmpty());
        HighlightedResult<Resource> hit = result.getResults().stream()
                .filter(r -> DatabaseConfiguration.TEST_RESOURCE_ID.equals(r.getResult().getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(hit.getHighlights().stream().anyMatch(h -> "bio".equals(h.getField())));
    }

    @Test
    void searchWithHighlights_returnsIndexedFieldHighlights() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("Jodeee");

        Paging<HighlightedResult<Resource>> result = searchService.searchWithHighlights(filter);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getResults().size());
        assertFalse(result.getResults().getFirst().getHighlights().isEmpty());
        assertNotEquals("payload", result.getResults().getFirst().getHighlights().getFirst().getField());
        assertTrue(result.getResults().getFirst().getHighlights().getFirst().getValue().contains("<em>Jodeee</em>"));
    }

    @Test
    void searchWithHighlights_boostsResourcesWithMultipleHighlights() {
        HighlightScoreFixture fixture = createHighlightScoreFixture();

        FacetFilter filter = employeeFilter();
        filter.setKeyword("Data");

        Paging<HighlightedResult<Resource>> result = searchService.searchWithHighlights(filter);

        assertEquals(2, result.getTotal());
        assertEquals(fixture.multiHighlight().getId(), result.getResults().getFirst().getResult().getId());
        assertEquals(2.0f, result.getResults().getFirst().getScore());
        assertEquals(fixture.singleHighlight().getId(), result.getResults().get(1).getResult().getId());
        assertEquals(1.0f, result.getResults().get(1).getScore());
    }

    @Test
    void searchWithHighlights_preservesExplicitSortOrderWhenScoringHighlights() {
        HighlightScoreFixture fixture = createHighlightScoreFixture();

        FacetFilter filter = employeeFilter();
        filter.setKeyword("Data");
        filter.addOrderBy("age", "asc");

        Paging<HighlightedResult<Resource>> result = searchService.searchWithHighlights(filter);

        assertEquals(2, result.getTotal());
        assertEquals(fixture.singleHighlight().getId(), result.getResults().getFirst().getResult().getId());
        assertEquals(1.0f, result.getResults().getFirst().getScore());
        assertEquals(fixture.multiHighlight().getId(), result.getResults().get(1).getResult().getId());
        assertEquals(2.0f, result.getResults().get(1).getScore());
    }

    @Test
    void resourceLifecycle_indexesChunksWithEmbeddingModel() {
        Resource created = resourceService.addResource(newEmployeeResource("Analytics Engineer", 34));
        resourceChunkIndexService.reindex(created);

        List<ResourceChunk> chunks = resourceChunkDao.findByResourceIdOrderByChunkIdxAsc(created.getId());

        assertFalse(chunks.isEmpty());
        assertEquals(0, chunks.getFirst().getChunkIdx());
        assertNotNull(chunks.getFirst().getFieldName());
        assertTrue(chunks.getFirst().getValueOrdinal() >= 0);
        assertEquals("test-embedding-model", chunks.getFirst().getEmbeddingModel());
    }

    @Test
    void semanticSearch_returnsResourcesByChunkSimilarity() {
        Resource analytics = resourceService.addResource(newEmployeeResource("Analytics Engineer", 34));
        Resource operations = resourceService.addResource(newEmployeeResource("Operations Lead", 41));
        resourceChunkIndexService.reindex(analytics);
        resourceChunkIndexService.reindex(operations);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");

        Paging<Resource> results = searchService.semanticSearch(filter);

        assertTrue(results.getTotal() >= 1);
        assertEquals(analytics.getId(), results.getResults().getFirst().getId());
    }

    @Test
    void semanticSearch_rejectsExplicitSortOrder() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("analytics");
        filter.addOrderBy("age", "asc");

        UnsupportedSearchParameterException exception = assertThrows(UnsupportedSearchParameterException.class,
                () -> searchService.semanticSearch(filter));

        assertEquals("Semantic search is relevance-ranked and does not support sort/order parameters.",
                exception.getMessage());
    }

    @Test
    void hybridSearchWithHighlights_returnsLexicalAndSemanticHighlights() {
        Resource hybrid = resourceService.addResource(newEmployeeResource("Data Analyst", 37));
        resourceChunkIndexService.reindex(hybrid);

        FacetFilter filter = employeeFilter();
        filter.setKeyword("Data");

        Paging<HighlightedResult<Resource>> results = searchService.hybridSearchWithHighlights(filter);

        assertFalse(results.getResults().isEmpty());
        HighlightedResult<Resource> first = results.getResults().stream()
                .filter(result -> hybrid.getId().equals(result.getResult().getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(first.getHighlights().stream()
                .anyMatch(highlight -> !"payload".equals(highlight.getField()) && highlight.getValue().contains("<em>")));
        assertFalse(first.getHighlights().isEmpty());
    }

    @Test
    void hybridSearchWithHighlights_rejectsExplicitSortOrder() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("Data");
        filter.addOrderBy("age", "asc");

        UnsupportedSearchParameterException exception = assertThrows(UnsupportedSearchParameterException.class,
                () -> searchService.hybridSearchWithHighlights(filter));

        assertEquals("Hybrid search is relevance-ranked and does not support sort/order parameters.",
                exception.getMessage());
    }

    @Test
    void hybridSearch_rejectsExplicitSortOrder() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("Data");
        filter.addOrderBy("age", "asc");

        UnsupportedSearchParameterException exception = assertThrows(UnsupportedSearchParameterException.class,
                () -> searchService.hybridSearch(filter));

        assertEquals("Hybrid search is relevance-ranked and does not support sort/order parameters.",
                exception.getMessage());
    }

    @Test
    void recommend_returnsSimilarResourcesOrderedByChunkSimilarity() {
        Resource source = resourceService.getResource(DatabaseConfiguration.TEST_RESOURCE_ID);
        source.setPayload(newEmployeePayload("Analytics Source", 28));
        source = resourceService.updateResource(source);
        resourceChunkIndexService.reindex(source);

        Resource similar = resourceService.addResource(newEmployeeResource("Analytics Peer", 28));
        Resource operations = resourceService.addResource(newEmployeeResource("Operations Peer", 28));
        resourceChunkIndexService.reindex(similar);
        resourceChunkIndexService.reindex(operations);

        FacetFilter filter = employeeFilter();
        List<ScoredResult<Resource>> recommendations = searchService.recommend(
                filter,
                new SearchService.KeyValue("first_name", "Analytics Source")
        );

        assertFalse(recommendations.isEmpty());
        assertEquals(similar.getId(), recommendations.getFirst().getResult().getId());
    }

    @Test
    void recommend_acceptsSinglePrimaryKey() {
        Resource source = resourceService.addResource(newEmployeeResource("Public Source", 28));
        Resource similar = resourceService.addResource(newEmployeeResource("Public Peer", 28));
        resourceChunkIndexService.reindex(source);
        resourceChunkIndexService.reindex(similar);

        FacetFilter filter = employeeFilter();
        List<ScoredResult<Resource>> recommendations = searchService.recommend(
                filter,
                new SearchService.KeyValue("first_name", "Public Source")
        );

        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().noneMatch(sr -> source.getId().equals(sr.getResult().getId())));
    }

    @Test
    void recommend_rejectsResourceWithoutEmbeddingChunks() {
        Resource source = resourceService.addResource(newEmployeeResource("Unindexed Source", 28));
        resourceChunkDao.deleteByResourceId(source.getId());

        FacetFilter filter = employeeFilter();
        MissingResourceEmbeddingsException exception = assertThrows(MissingResourceEmbeddingsException.class,
                () -> searchService.recommend(
                        filter,
                        new SearchService.KeyValue("first_name", "Unindexed Source")
                ));

        assertEquals(
                "Resource [id=%s] has no embeddings; embedding-backed operations are unavailable.".formatted(source.getId()),
                exception.getMessage()
        );
    }

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

    private HighlightScoreFixture createHighlightScoreFixture() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Resource singleHighlight = resourceService.addResource(newEmployeeResource("Data Analyst", 30));
        ResourceType employee = resourceTypeService.getResourceType("employee");
        List<IndexField> fields = employee.getIndexFields();
        fields.add(indexField("alternate_name", employee));
        employee.setIndexFields(fields);
        Resource multiHighlight = resourceService.addResource(newEmployeeResource("Data Engineer", 31));
        jdbcTemplate.update("""
                INSERT INTO public.stringindexedfield (id, name, resource_id)
                VALUES (?, ?, ?)
                """,
                551230L, "alternate_name", multiHighlight.getId());
        jdbcTemplate.update("""
                INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
                VALUES (?, ?)
                """,
                551230L, "Data Engineer");
        viewService.deleteView("employee");
        viewService.createView(employee);
        return new HighlightScoreFixture(singleHighlight, multiHighlight);
    }

    private IndexField indexField(String name, ResourceType resourceType) {
        IndexField indexField = new IndexField();
        indexField.setName(name);
        indexField.setLabel(name);
        indexField.setPath("//*[local-name()='author']/text()");
        indexField.setType("java.lang.String");
        indexField.setResourceType(resourceType);
        return indexField;
    }

    private IndexField textOnlyIndexField(String name, ResourceType resourceType) {
        IndexField indexField = indexField(name, resourceType);
        indexField.setSearchCapabilities(EnumSet.of(SearchCapability.TEXT));
        return indexField;
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

    private float[] embeddingFor(String text) {
        String normalized = text == null ? "" : text.toLowerCase();
        if (normalized.contains("analytics") || normalized.contains("data")) {
            return vector(1f, 0f, 0f);
        }
        if (normalized.contains("operations")) {
            return vector(0f, 1f, 0f);
        }
        if (normalized.contains("jodeee")) {
            return vector(0f, 0f, 1f);
        }
        return vector(0.2f, 0.2f, 0.2f);
    }

    private float[] vector(float x, float y, float z) {
        float[] vector = new float[EmbeddingService.VECTOR_SIZE];
        vector[0] = x;
        vector[1] = y;
        vector[2] = z;
        return vector;
    }

    private record HighlightScoreFixture(Resource singleHighlight, Resource multiHighlight) {
    }
}
