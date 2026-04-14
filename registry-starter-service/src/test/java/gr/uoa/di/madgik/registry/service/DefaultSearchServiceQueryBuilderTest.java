package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSearchServiceQueryBuilderTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

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
    void createEmployeeView() {
        viewService.createView(resourceTypeService.getResourceType("employee"));
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
    void search_multivaluedField_usesArrayContainsPredicate() {
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

    private FacetFilter employeeFilter() {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType("employee");
        filter.setQuantity(10);
        return filter;
    }
}
