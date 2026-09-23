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
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for the connection leak in the multivalued-field ("array contains") search
 * predicate: {@code SearchSqlQueryBuilder#bindPostgresArray} used to obtain a JDBC {@link java.sql.Connection}
 * without closing it on any path, so every search filtering on a multivalued field permanently
 * removed one connection from the pool. Runs against a deliberately tiny pool so a regression
 * fails fast (connection-acquisition timeout) instead of hanging for the default 30s.
 */
@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSearchServiceConnectionLeakTest extends PostgreSqlTestContainerSupport {

    private static final int POOL_SIZE = 2;
    private static final int SEARCHES_TO_RUN = POOL_SIZE + 5;

    @DynamicPropertySource
    static void shrinkPool(DynamicPropertyRegistry registry) {
        registry.add("registry.datasource.configuration.maximum-pool-size", () -> POOL_SIZE);
        registry.add("registry.datasource.configuration.connection-timeout", () -> 2000);
    }

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
    @Qualifier("registryDataSource")
    DataSource dataSource;

    @BeforeAll
    void addMultivaluedFieldAndView() {
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
                923129L, "tags", DatabaseConfiguration.TEST_RESOURCE_ID);
        jdbcTemplate.batchUpdate("""
                        INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
                        VALUES (?, ?)
                        """,
                List.of(new Object[]{923129L, "alpha"}, new Object[]{923129L, "beta"}));

        viewService.createView(resourceTypeService.getResourceType("employee"));
    }

    @BeforeEach
    void stubEmbeddingModel() {
        when(auditActorProvider.currentActor()).thenReturn("leak-test");
        lenient().when(embeddingService.embed(anyString())).thenReturn(new float[EmbeddingService.VECTOR_SIZE]);
        lenient().when(embeddingService.modelName()).thenReturn("test-embedding-model");
    }

    @Test
    void search_withMultivaluedFieldFilter_doesNotLeakConnections() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < SEARCHES_TO_RUN; i++) {
                FacetFilter filter = new FacetFilter();
                filter.setResourceType("employee");
                filter.setQuantity(10);
                filter.addFilter("tags", List.of("beta"));

                Paging<Resource> result = searchService.search(filter);
                assertEquals(1, result.getTotal());
            }
        }, "search() with a multivalued-field filter should release its JDBC connection on every call, "
                + "not just exhaust a " + POOL_SIZE + "-connection pool after " + POOL_SIZE + " calls");
    }
}
