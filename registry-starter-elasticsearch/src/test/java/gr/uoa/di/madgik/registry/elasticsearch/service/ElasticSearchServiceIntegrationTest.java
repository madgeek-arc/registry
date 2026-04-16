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
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.elasticsearch.autoconfigure.RegistryElasticsearchProperties;
import gr.uoa.di.madgik.registry.elasticsearch.service.ElasticIndexFieldsResolver;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.StringReader;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticSearchServiceIntegrationTest {

    @Container
    static final GenericContainer<?> ELASTICSEARCH = new GenericContainer<>(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.3.2"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
            .withExposedPorts(9200)
            .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forHttp("/")
                    .forPort(9200)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private ElasticsearchClient client;
    private Rest5Client restClient;
    private ElasticsearchTransport transport;
    private EmbeddingService embeddingService;
    private ElasticSearchService searchService;

    @BeforeAll
    void setUp() {
        URI uri = URI.create("http://%s:%d".formatted(
                ELASTICSEARCH.getHost(),
                ELASTICSEARCH.getMappedPort(9200)
        ));
        restClient = Rest5Client.builder(uri).build();
        transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
        client = new ElasticsearchClient(transport);

        embeddingService = mock(EmbeddingService.class);
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        when(resourceTypeService.getResourceType(anyString())).thenAnswer(invocation -> {
            ResourceType resourceType = new ResourceType();
            resourceType.setName(invocation.getArgument(0, String.class));
            resourceType.setIndexFields(new ArrayList<>());
            return resourceType;
        });
        when(resourceTypeService.getIndexFieldLabels(anyString())).thenReturn(Map.of());
        when(resourceTypeService.getAllResourceTypeByAlias(anyString())).thenReturn(List.of());

        RegistryElasticsearchProperties elasticsearchProperties = new RegistryElasticsearchProperties();
        elasticsearchProperties.getAggregation().setTopHitsSize(100);
        elasticsearchProperties.getAggregation().setBucketSize(100);
        elasticsearchProperties.getIndex().setMaxResultWindow(1000);
        elasticsearchProperties.getSearch().getHighlight().setFragmentSize(400);
        elasticsearchProperties.getSearch().getHighlight().setNumberOfFragments(5);

        searchService = new ElasticSearchService(
                client,
                new JacksonJsonpMapper(objectMapper),
                embeddingService,
                resourceTypeService,
                elasticsearchProperties,
                new ElasticIndexFieldsResolver(client)
        );
    }

    @AfterAll
    void tearDown() throws Exception {
        if (transport != null) {
            transport.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }

    @Test
    void semanticSearch_returnsNearestNeighbors_and_ignoresMissingEmbeddings() throws Exception {
        String index = createIndex();
        indexDocument(index, "exact", "exact semantic match", List.of(1.0f, 0.0f, 0.0f));
        indexDocument(index, "near", "near semantic match", List.of(0.9f, 0.1f, 0.0f));
        indexDocument(index, "opposite", "opposite direction", List.of(-1.0f, 0.0f, 0.0f));
        indexDocumentWithoutEmbedding(index, "lexical-only", "semantic phrase without vector");

        when(embeddingService.embed("semantic phrase")).thenReturn(new float[]{1.0f, 0.0f, 0.0f});

        FacetFilter filter = filter(index, "semantic phrase");
        Paging<Resource> results = searchService.semanticSearch(filter);
        List<String> ids = results.getResults().stream().map(Resource::getId).toList();

        assertEquals("exact", ids.getFirst());
        assertTrue(ids.contains("near"));
        assertFalse(ids.contains("lexical-only"));
    }

    @Test
    void hybridSearch_combinesLexicalAndSemanticMatches() throws Exception {
        String index = createIndex();
        indexDocument(index, "both", "registry handbook", List.of(1.0f, 0.0f, 0.0f));
        indexDocument(index, "semantic-only", "unrelated payload", List.of(1.0f, 0.0f, 0.0f));
        indexDocumentWithoutEmbedding(index, "lexical-only", "registry manual");

        when(embeddingService.embed("registry")).thenReturn(new float[]{1.0f, 0.0f, 0.0f});

        FacetFilter filter = filter(index, "registry");
        Paging<Resource> results = searchService.hybridSearch(filter);
        List<String> ids = results.getResults().stream().map(Resource::getId).toList();

        assertTrue(ids.contains("both"));
        assertTrue(ids.contains("semantic-only"));
        assertTrue(ids.contains("lexical-only"));
    }

    @Test
    void recommend_returnsNearestNeighbors_and_excludesSource() throws Exception {
        String index = createIndex();
        indexDocument(index, "source", "source payload", List.of(1.0f, 0.0f, 0.0f));
        indexDocument(index, "neighbor", "neighbor payload", List.of(0.9f, 0.1f, 0.0f));
        indexDocument(index, "opposite", "opposite payload", List.of(-1.0f, 0.0f, 0.0f));
        indexDocumentWithoutEmbedding(index, "missing", "missing embedding");

        FacetFilter filter = filter(index, null);
        List<Resource> results = searchService.recommend(filter, new SearchService.KeyValue("id", "source"));
        List<String> ids = results.stream().map(Resource::getId).toList();

        assertEquals("neighbor", ids.getFirst());
        assertFalse(ids.contains("source"));
        assertFalse(ids.contains("missing"));
    }

    @Test
    void semanticSearch_emptyKeyword_throwsServiceException() throws Exception {
        String index = createIndex();
        indexDocument(index, "doc1", "some content", List.of(1.0f, 0.0f, 0.0f));

        FacetFilter filter = filter(index, null);
        assertThrows(ServiceException.class, () -> searchService.semanticSearch(filter));
    }

    @Test
    void hybridSearch_emptyKeyword_returnsAllDocuments() throws Exception {
        String index = createIndex();
        indexDocument(index, "doc-a", "first document", List.of(1.0f, 0.0f, 0.0f));
        indexDocument(index, "doc-b", "second document", List.of(0.0f, 1.0f, 0.0f));

        FacetFilter filter = filter(index, null);
        Paging<Resource> results = searchService.hybridSearch(filter);

        assertEquals(2, results.getTotal());
        List<String> ids = results.getResults().stream().map(Resource::getId).toList();
        assertTrue(ids.contains("doc-a"));
        assertTrue(ids.contains("doc-b"));
    }

    @Test
    void semanticSearch_withTermFilter_narrowsResults() throws Exception {
        String index = createIndexWithStatusField();
        indexDocumentWithStatus(index, "active-doc", "query content", "active", List.of(1.0f, 0.0f, 0.0f));
        indexDocumentWithStatus(index, "archived-doc", "query content", "archived", List.of(1.0f, 0.0f, 0.0f));

        when(embeddingService.embed("query content")).thenReturn(new float[]{1.0f, 0.0f, 0.0f});

        FacetFilter filter = filter(index, "query content");
        filter.addFilter("status", "active");
        Paging<Resource> results = searchService.semanticSearch(filter);

        List<String> ids = results.getResults().stream().map(Resource::getId).toList();
        assertTrue(ids.contains("active-doc"));
        assertFalse(ids.contains("archived-doc"));
    }

    @Test
    void hybridSearch_withTermFilter_narrowsResults() throws Exception {
        String index = createIndexWithStatusField();
        indexDocumentWithStatus(index, "active-doc", "query content", "active", List.of(1.0f, 0.0f, 0.0f));
        indexDocumentWithStatus(index, "archived-doc", "query content", "archived", List.of(1.0f, 0.0f, 0.0f));

        when(embeddingService.embed("query content")).thenReturn(new float[]{1.0f, 0.0f, 0.0f});

        FacetFilter filter = filter(index, "query content");
        filter.addFilter("status", "active");
        Paging<Resource> results = searchService.hybridSearch(filter);

        List<String> ids = results.getResults().stream().map(Resource::getId).toList();
        assertTrue(ids.contains("active-doc"));
        assertFalse(ids.contains("archived-doc"));
    }

    @Test
    void searchWithHighlights_returnsHighlightFragments() throws Exception {
        String index = createIndexWithStatusField();
        indexDocumentWithStatus(index, "highlight-doc", "unique searchable phrase here", "active",
                List.of(1.0f, 0.0f, 0.0f));

        FacetFilter filter = filter(index, "unique searchable phrase");
        Paging<HighlightedResult<Resource>> results = searchService.searchWithHighlights(filter);

        assertFalse(results.getResults().isEmpty());
        boolean hasHighlights = results.getResults().stream()
                .anyMatch(r -> r.getHighlights() != null && !r.getHighlights().isEmpty());
        assertTrue(hasHighlights);
    }

    @Test
    void hybridSearchWithHighlights_returnsHighlightFragments() throws Exception {
        String index = createIndexWithStatusField();
        indexDocumentWithStatus(index, "highlight-doc", "unique searchable phrase here", "active",
                List.of(1.0f, 0.0f, 0.0f));

        when(embeddingService.embed("unique searchable phrase")).thenReturn(new float[]{1.0f, 0.0f, 0.0f});

        FacetFilter filter = filter(index, "unique searchable phrase");
        Paging<HighlightedResult<Resource>> results = searchService.hybridSearchWithHighlights(filter);

        assertFalse(results.getResults().isEmpty());
        boolean hasHighlights = results.getResults().stream()
                .anyMatch(r -> r.getHighlights() != null && !r.getHighlights().isEmpty());
        assertTrue(hasHighlights);
    }

    @Test
    void recommend_sourceHasNoEmbedding_throwsResourceNotFoundException() throws Exception {
        String index = createIndex();
        indexDocumentWithoutEmbedding(index, "source-no-embed", "source payload");
        indexDocument(index, "neighbor", "neighbor payload", List.of(0.9f, 0.1f, 0.0f));

        FacetFilter filter = filter(index, null);
        assertThrows(ResourceNotFoundException.class,
                () -> searchService.recommend(filter, new SearchService.KeyValue("id", "source-no-embed")));
    }

    @Test
    void semanticSearch_withAllZeroEmbedding_returnsNoResults() throws Exception {
        String index = createIndex();
        indexDocument(index, "valid-doc", "some meaningful content", List.of(1.0f, 0.0f, 0.0f));

        when(embeddingService.embed("zero query")).thenReturn(new float[]{0.0f, 0.0f, 0.0f});

        FacetFilter filter = filter(index, "zero query");
        Paging<Resource> results = searchService.semanticSearch(filter);

        assertEquals(0, results.getTotal());
        assertTrue(results.getResults() == null || results.getResults().isEmpty());
    }

    private String createIndex() throws Exception {
        String index = "semantic-" + UUID.randomUUID().toString().replace("-", "");
        String mapping = objectMapper.writeValueAsString(Map.of(
                "mappings", Map.of(
                        "properties", Map.of(
                                "id", Map.of("type", "keyword"),
                                "payload", Map.of("type", "text"),
                                "searchableArea", Map.of("type", "text"),
                                "payloadFormat", Map.of("type", "keyword"),
                                "version", Map.of("type", "keyword"),
                                "embedding", Map.of(
                                        "type", "dense_vector",
                                        "dims", 3,
                                        "index", true,
                                        "similarity", "cosine"
                                )
                        )
                )
        ));

        client.indices().create(c -> c.index(index).withJson(new StringReader(mapping)));
        return index;
    }

    private void indexDocument(String index, String id, String payload, List<Float> embedding) throws Exception {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", id);
        document.put("payload", payload);
        document.put("searchableArea", payload);
        document.put("payloadFormat", "json");
        document.put("version", "1");
        document.put("embedding", embedding);
        client.index(i -> i.index(index).id(id).document(document).refresh(Refresh.True));
    }

    private void indexDocumentWithoutEmbedding(String index, String id, String payload) throws Exception {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", id);
        document.put("payload", payload);
        document.put("searchableArea", payload);
        document.put("payloadFormat", "json");
        document.put("version", "1");
        client.index(i -> i.index(index).id(id).document(document).refresh(Refresh.True));
    }

    private FacetFilter filter(String resourceType, String keyword) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setKeyword(keyword);
        filter.setFrom(0);
        filter.setQuantity(10);
        return filter;
    }

    /**
     * Creates an index with an extra {@code status} keyword field while leaving lexical search and
     * highlighting to the real text fields (`searchableArea` here) instead of synthetic text
     * sub-fields on every keyword field.
     */
    private String createIndexWithStatusField() throws Exception {
        String index = "semantic-" + UUID.randomUUID().toString().replace("-", "");
        String mapping = objectMapper.writeValueAsString(Map.of(
                "mappings", Map.of(
                        "properties", Map.of(
                                "id", Map.of("type", "keyword"),
                                "payload", Map.of("type", "keyword"),
                                "searchableArea", Map.of("type", "text"),
                                "payloadFormat", Map.of("type", "keyword"),
                                "version", Map.of("type", "keyword"),
                                "status", Map.of("type", "keyword"),
                                "embedding", Map.of(
                                        "type", "dense_vector",
                                        "dims", 3,
                                        "index", true,
                                        "similarity", "cosine"
                                )
                        )
                )
        ));

        client.indices().create(c -> c.index(index).withJson(new StringReader(mapping)));
        return index;
    }

    private void indexDocumentWithStatus(String index, String id, String payload, String status,
                                         List<Float> embedding) throws Exception {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", id);
        document.put("payload", payload);
        document.put("searchableArea", payload);
        document.put("payloadFormat", "json");
        document.put("version", "1");
        document.put("status", status);
        document.put("embedding", embedding);
        client.index(i -> i.index(index).id(id).document(document).refresh(Refresh.True));
    }
}
