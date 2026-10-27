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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
