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
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Re-runs the core search paths with {@code registry.sql.search.lexical-strategy=full-text}
 * active, verifying the {@code search_vector} generated column/GIN index added by the
 * {@code V6.0} migration and the {@link gr.uoa.di.madgik.registry.service.lexical.FullTextLexicalStrategy}
 * wiring work end-to-end against a real Postgres instance.
 */
@SpringBootTest(classes = DatabaseConfiguration.class,
        properties = {"spring.profiles.active=test", "registry.sql.search.lexical-strategy=full-text"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class DefaultSearchServiceFullTextStrategyTest extends PostgreSqlTestContainerSupport {

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

    @BeforeAll
    void createEmployeeView() {
        ResourceType resourceType = resourceTypeService.getResourceType("employee");
        viewService.createView(resourceType);
    }

    @BeforeEach
    void stubEmbeddingModel() {
        when(auditActorProvider.currentActor()).thenReturn("full-text-search-test");
        lenient().when(embeddingService.embed(anyString())).thenReturn(new float[EmbeddingService.VECTOR_SIZE]);
        lenient().when(embeddingService.modelName()).thenReturn("test-embedding-model");
    }

    @Test
    void search_matchesOnSingleSignificantTerm() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("Jodeee");

        Paging<Resource> result = searchService.search(filter);

        assertEquals(1, result.getTotal());
        assertEquals(DatabaseConfiguration.TEST_RESOURCE_ID, result.getResults().getFirst().getId());
    }

    @Test
    void search_stopWordOnlyQuery_returnsNoMatches() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("the of");

        Paging<Resource> result = searchService.search(filter);

        assertEquals(0, result.getTotal());
    }

    @Test
    void searchWithHighlights_returnsIndexedFieldHighlights() {
        FacetFilter filter = employeeFilter();
        filter.setKeyword("Jodeee");

        Paging<HighlightedResult<Resource>> result = searchService.searchWithHighlights(filter);

        assertEquals(1, result.getTotal());
        assertFalse(result.getResults().getFirst().getHighlights().isEmpty());
        assertTrue(result.getResults().getFirst().getHighlights().getFirst().getValue().contains("<em>Jodeee</em>"));
    }

    private FacetFilter employeeFilter() {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType("employee");
        filter.setQuantity(10);
        return filter;
    }
}
