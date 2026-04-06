package gr.uoa.di.madgik.registry.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RangeFilterTest {

    @Test
    void constructor_storesAllFields() {
        Date from = new Date(1000L);
        Date to = new Date(2000L);

        RangeFilter rf = new RangeFilter(from, to, true);

        assertSame(from, rf.getFrom());
        assertSame(to, rf.getTo());
        assertTrue(rf.isIncludeNull());
    }

    @Test
    void constructor_allowsNullBounds() {
        RangeFilter rf = new RangeFilter(null, null, false);

        assertNull(rf.getFrom());
        assertNull(rf.getTo());
        assertFalse(rf.isIncludeNull());
    }

    @Test
    void constructor_allowsNullLowerBound() {
        Date to = new Date(5000L);
        RangeFilter rf = new RangeFilter(null, to, true);

        assertNull(rf.getFrom());
        assertSame(to, rf.getTo());
    }

    @Test
    void constructor_allowsNullUpperBound() {
        Date from = new Date(1000L);
        RangeFilter rf = new RangeFilter(from, null, false);

        assertSame(from, rf.getFrom());
        assertNull(rf.getTo());
    }

    // -------------------------------------------------------------------------
    // FacetFilter.addRangeFilter integration
    // -------------------------------------------------------------------------

    @Test
    void facetFilter_addRangeFilter_storesEntry() {
        FacetFilter filter = new FacetFilter();
        Date from = new Date(1000L);
        Date to = new Date(2000L);

        filter.addRangeFilter("publishDate", from, to, true);

        assertEquals(1, filter.getRangeFilters().size());
        RangeFilter rf = filter.getRangeFilters().get("publishDate");
        assertNotNull(rf);
        assertSame(from, rf.getFrom());
        assertSame(to, rf.getTo());
        assertTrue(rf.isIncludeNull());
    }

    @Test
    void facetFilter_addRangeFilter_multipleFields() {
        FacetFilter filter = new FacetFilter();

        filter.addRangeFilter("publishDate", null, new Date(), true);
        filter.addRangeFilter("expiryDate", new Date(), null, true);

        assertEquals(2, filter.getRangeFilters().size());
        assertTrue(filter.getRangeFilters().containsKey("publishDate"));
        assertTrue(filter.getRangeFilters().containsKey("expiryDate"));
    }

    @Test
    void facetFilter_addRangeFilter_overwritesPreviousEntry() {
        FacetFilter filter = new FacetFilter();
        Date firstTo = new Date(1000L);
        Date secondTo = new Date(9999L);

        filter.addRangeFilter("publishDate", null, firstTo, false);
        filter.addRangeFilter("publishDate", null, secondTo, true);

        assertEquals(1, filter.getRangeFilters().size());
        assertSame(secondTo, filter.getRangeFilters().get("publishDate").getTo());
    }

    @Test
    void facetFilter_rangeFiltersInitiallyEmpty() {
        FacetFilter filter = new FacetFilter();

        assertNotNull(filter.getRangeFilters());
        assertTrue(filter.getRangeFilters().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Jackson round-trip
    // -------------------------------------------------------------------------

    @Test
    void jacksonRoundTrip_rangeFilterSurvivesSerializeDeserialize() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        FacetFilter original = new FacetFilter();
        original.addRangeFilter("publishDate", 1000L, 5000L, true);

        String json = mapper.writeValueAsString(original);
        FacetFilter restored = mapper.readValue(json, FacetFilter.class);

        assertEquals(1, restored.getRangeFilters().size());
        RangeFilter rf = restored.getRangeFilters().get("publishDate");
        assertNotNull(rf);
        assertTrue(rf.isIncludeNull());
    }

    @Test
    void jacksonRoundTrip_nullBoundsSurvive() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        FacetFilter original = new FacetFilter();
        original.addRangeFilter("expiryDate", null, null, true);

        String json = mapper.writeValueAsString(original);
        FacetFilter restored = mapper.readValue(json, FacetFilter.class);

        RangeFilter rf = restored.getRangeFilters().get("expiryDate");
        assertNotNull(rf);
        assertNull(rf.getFrom());
        assertNull(rf.getTo());
        assertTrue(rf.isIncludeNull());
    }

    @Test
    void facetFilter_addFilter_doesNotAffectRangeFilters() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("status", "APPROVED");
        filter.addRangeFilter("publishDate", null, new Date(), true);

        assertEquals(1, filter.getFilter().size());
        assertEquals(1, filter.getRangeFilters().size());
    }
}
