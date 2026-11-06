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

package gr.uoa.di.madgik.registry.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacetFilterTest {

    @Test
    void addFilterStoresValue() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("type", "eu");
        filter.addFilter("tags", List.of("a", "b"));

        assertEquals("eu", filter.getFilter().get("type"));
        assertEquals(List.of("a", "b"), filter.getFilter().get("tags"));
    }

    @Test
    void addFilterIgnoresNullValue() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("type", null);

        assertFalse(filter.getFilter().containsKey("type"));
    }

    @Test
    void addFilterIgnoresEmptyString() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("type", "");

        assertFalse(filter.getFilter().containsKey("type"));
    }

    @Test
    void fromMarksRequestFiltersAsSelections() {
        FacetFilter filter = FacetFilter.from(params("trl", "7", "keyword", "data"));

        assertEquals(Set.of("trl"), filter.getSelectionFields());
        assertEquals(Map.of("trl", List.of("7")), filter.getSelectionFilters());
        assertTrue(filter.getFixedFilters().isEmpty());
    }

    @Test
    void addFilterIsFixedByDefault() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("active", true);

        assertEquals(Map.of("active", true), filter.getFixedFilters());
        assertTrue(filter.getSelectionFilters().isEmpty());
    }

    @Test
    void addFilterFixesAFieldSentByTheClient() {
        FacetFilter filter = FacetFilter.from(params("active", "false"));
        filter.addFilter("active", true);

        assertEquals(Map.of("active", true), filter.getFixedFilters());
        assertTrue(filter.getSelectionFilters().isEmpty());
    }

    @Test
    void addFilterWithFixedFalseAddsASelection() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("trl", "7", false);

        assertEquals(Map.of("trl", "7"), filter.getSelectionFilters());
        assertTrue(filter.getFixedFilters().isEmpty());
    }

    @Test
    void addFilterWithFixedFalseUnfixesAField() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("trl", "7");
        filter.addFilter("trl", "8", false);

        assertEquals(Map.of("trl", "8"), filter.getSelectionFilters());
        assertTrue(filter.getFixedFilters().isEmpty());
    }

    @Test
    void addFilterWithIgnoredValueKeepsFilterAndMark() {
        FacetFilter filter = FacetFilter.from(params("trl", "7"));
        filter.addFilter("trl", null);
        filter.addFilter("trl", "");

        assertEquals(Map.of("trl", List.of("7")), filter.getSelectionFilters());
    }

    @Test
    void selectionFieldsTakePartInEquality() {
        FacetFilter fixed = new FacetFilter();
        fixed.addFilter("trl", "7");
        FacetFilter selection = new FacetFilter();
        selection.addFilter("trl", "7", false);

        assertNotEquals(fixed, selection);
        assertNotEquals(fixed.hashCode(), selection.hashCode());
    }

    @Test
    void setSelectionFieldsAcceptsNull() {
        FacetFilter filter = new FacetFilter();
        filter.setSelectionFields(null);
        filter.addFilter("trl", "7", false);

        assertEquals(Set.of("trl"), filter.getSelectionFields());
    }

    @Test
    void fromKeepsFiltersNamedByFixedFieldsAsFixed() {
        FacetFilter filter = FacetFilter.from(params("published", "true", "trl", "7",
                FacetFilter.FIXED_FIELDS_PARAMETER, "published"));

        assertEquals(Map.of("published", List.of("true")), filter.getFixedFilters());
        assertEquals(Map.of("trl", List.of("7")), filter.getSelectionFilters());
        assertFalse(filter.getFilter().containsKey(FacetFilter.FIXED_FIELDS_PARAMETER));
    }

    @Test
    void fromSplitsCommaSeparatedBrowseBy() {
        Map<String, List<Object>> params = new HashMap<>();
        params.put("browseBy", new ArrayList<>(List.of("trl, users", "portfolios")));

        assertEquals(List.of("trl", "users", "portfolios"), FacetFilter.from(params).getBrowseBy());
    }

    private static Map<String, List<Object>> params(String... keysAndValues) {
        Map<String, List<Object>> params = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            params.put(keysAndValues[i], new ArrayList<>(List.of(keysAndValues[i + 1])));
        }
        return params;
    }
}
