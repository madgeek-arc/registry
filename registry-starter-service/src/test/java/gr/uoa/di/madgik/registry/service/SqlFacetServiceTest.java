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
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Value;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
class SqlFacetServiceTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    SqlFacetService sqlFacetService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    ViewService viewService;

    @Autowired
    @Qualifier("registryDataSource")
    DataSource dataSource;

    @Test
    void createFacets_returnsScalarFacetBuckets() {
        refreshEmployeeView();
        FacetFilter filter = employeeFilter();
        SearchSqlQueryBuilder.SearchSqlQuery sqlQuery = searchQuery(filter);

        List<Facet> facets = sqlFacetService.createFacets(List.of("age"),
                List.of(resourceTypeService.getResourceType("employee")), sqlQuery);

        assertEquals(1, facets.size());
        Facet facet = facets.getFirst();
        assertEquals("age", facet.getField());
        assertEquals(1, facet.getValues().size());
        Value value = facet.getValues().getFirst();
        assertEquals("28", value.getValue());
        assertEquals(1L, value.getCount());
    }

    @Test
    void createFacets_returnsBucketsForMultivaluedField() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("""
                INSERT INTO public.indexfield
                (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
                VALUES (?, NULL, ?, true, ?, false, ?, ?)
                """,
                "tags", "tags", "//*[local-name()='tag']/text()", "java.lang.String", "employee");
        jdbcTemplate.update("""
                INSERT INTO public.resourcetype_indexfield
                (resourcetype_name, indexfields_resourcetype_name, indexfields_name)
                VALUES (?, ?, ?)
                """,
                "employee", "employee", "tags");
        jdbcTemplate.update("""
                INSERT INTO public.stringindexedfield (id, name, resource_id)
                VALUES (?, ?, ?)
                """,
                98321L, "tags", DatabaseConfiguration.TEST_RESOURCE_ID);
        jdbcTemplate.batchUpdate("""
                        INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
                        VALUES (?, ?)
                        """,
                List.of(new Object[]{98321L, "alpha"}, new Object[]{98321L, "beta"}));

        refreshEmployeeView();
        ResourceType employee = resourceTypeService.getResourceType("employee");

        FacetFilter filter = employeeFilter();
        filter.addFilter("tags", List.of("beta"));
        SearchSqlQueryBuilder.SearchSqlQuery sqlQuery = searchQuery(filter);

        List<Facet> facets = sqlFacetService.createFacets(List.of("tags"), List.of(employee), sqlQuery);

        assertEquals(1, facets.size());
        Facet facet = facets.getFirst();
        assertEquals("tags", facet.getField());
        assertEquals(2, facet.getValues().size());
        Map<String, Long> counts = facet.getValues().stream()
                .collect(Collectors.toMap(Value::getValue, Value::getCount));
        assertEquals(Map.of("alpha", 1L, "beta", 1L), counts);
    }

    private SearchSqlQueryBuilder.SearchSqlQuery searchQuery(FacetFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("keyword", "%");
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());

        return SearchSqlQueryBuilder.builder(dataSource)
                .withFilter(filter)
                .withResourceTypes(List.of(resourceTypeService.getResourceType("employee")))
                .withParameters(params)
                .buildSearchQuery();
    }

    private FacetFilter employeeFilter() {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType("employee");
        filter.setQuantity(10);
        return filter;
    }

    private void refreshEmployeeView() {
        viewService.deleteView("employee");
        viewService.createView(resourceTypeService.getResourceType("employee"));
    }
}
