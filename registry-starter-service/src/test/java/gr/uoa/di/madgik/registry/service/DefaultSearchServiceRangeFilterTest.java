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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for range filter support in {@link DefaultSearchService}.
 * Uses the {@code employee} resource type from the test fixture, which has:
 * <ul>
 *   <li>{@code age}      — bigint  (value: 28)</li>
 *   <li>{@code birthday} — timestamp (value: 1990-06-16 17:00:21)</li>
 * </ul>
 */
@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSearchServiceRangeFilterTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    SearchService searchService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    ViewService viewService;

    /**
     * Creates the {@code employee_view} after {@code data.sql} has populated the DB.
     */
    @BeforeAll
    void createEmployeeView() {
        ResourceType resourceType = resourceTypeService.getResourceType("employee");
        viewService.createView(resourceType);
    }

    // -------------------------------------------------------------------------
    // Integer range (age = 28)
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            //"from, to, hits"
            "20, 30, 1", // both bounds hits record
            "30,  , 0",  // lower bound above value
            ", 20, 0",   // upper bound below value
            "20, , 1",   // lower bound hits record
            ", 30, 1"    // upper bound hist record
    })
    void search_integerRange(Integer from, Integer to, int expected) {
        FacetFilter filter = employeeFilter();
        filter.addRangeFilter("age", from, to, false);

        Paging<Resource> result = searchService.search(filter);

        assertEquals(expected, result.getTotal());
    }

    // -------------------------------------------------------------------------
    // Date range (birthday = 1990-06-16 17:00:21)
    // -------------------------------------------------------------------------

    @Test
    void search_dateRange_bothBounds_hitsRecord() {
        FacetFilter filter = employeeFilter();
        filter.addRangeFilter("birthday",
                ts("1985-01-01T00:00:00Z"),
                ts("1995-01-01T00:00:00Z"),
                false);

        Paging<Resource> result = searchService.search(filter);

        assertEquals(1, result.getTotal());
    }

    @Test
    void search_dateRange_lowerBoundAfterValue_returnsEmpty() {
        FacetFilter filter = employeeFilter();
        filter.addRangeFilter("birthday",
                ts("2000-01-01T00:00:00Z"),
                null,
                false);

        Paging<Resource> result = searchService.search(filter);

        assertEquals(0, result.getTotal());
    }

    @Test
    void search_dateRange_upperBoundBeforeValue_returnsEmpty() {
        FacetFilter filter = employeeFilter();
        filter.addRangeFilter("birthday",
                null,
                ts("1980-01-01T00:00:00Z"),
                false);

        Paging<Resource> result = searchService.search(filter);

        assertEquals(0, result.getTotal());
    }

    // -------------------------------------------------------------------------
    // Combined equality + range
    // -------------------------------------------------------------------------

    @Test
    void search_equalityAndRange_bothMatch_hitsRecord() {
        FacetFilter filter = employeeFilter();
        filter.addFilter("single", false);
        filter.addRangeFilter("age", 20L, 30L, false);

        Paging<Resource> result = searchService.search(filter);

        assertEquals(1, result.getTotal());
    }

    @Test
    void search_equalityAndRange_rangeMisses_returnsEmpty() {
        FacetFilter filter = employeeFilter();
        filter.addFilter("single", false);
        filter.addRangeFilter("age", 50L, 100L, false);

        Paging<Resource> result = searchService.search(filter);

        assertEquals(0, result.getTotal());
    }

    // -------------------------------------------------------------------------
    // Security — field name sanitization
    // -------------------------------------------------------------------------

    @Test
    void search_maliciousFieldName_strippedToEmpty_doesNotThrow() {
        FacetFilter filter = employeeFilter();
        // After stripping [^A-Za-z0-9_], this becomes "" and must be skipped
        filter.addRangeFilter("; DROP TABLE employee_view--", 20L, 30L, false);

        // Should not throw a SQL error and should return the full unfiltered result set
        Paging<Resource> result = searchService.search(filter);
        assertEquals(1, result.getTotal());
    }

    @Test
    void search_fieldNameWithSpecialChars_onlyAlphanumericPortionUsed() {
        FacetFilter filter = employeeFilter();
        // "age!!!" sanitizes to "age" — the range still applies
        filter.addRangeFilter("age!!!", 20L, 30L, false);

        Paging<Resource> result = searchService.search(filter);
        assertEquals(1, result.getTotal());
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

    private Timestamp ts(String iso) {
        return Timestamp.from(Instant.parse(iso));
    }
}
