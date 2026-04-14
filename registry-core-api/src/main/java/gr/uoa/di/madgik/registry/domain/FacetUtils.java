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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Shared helpers for assembling {@link Facet} objects consistently across backends.
 */
public final class FacetUtils {

    private static final Comparator<Value> FACET_VALUE_ORDER =
            Comparator.comparingLong(Value::getCount)
                    .reversed()
                    .thenComparing(Value::getValue, Comparator.nullsLast(String::compareTo));

    private FacetUtils() {
    }

    public static List<Facet> createFacets(List<String> browseBy,
                                           UnaryOperator<String> labelResolver,
                                           Function<String, List<Value>> valuesResolver) {
        if (browseBy == null || browseBy.isEmpty()) {
            return new ArrayList<>();
        }
        List<Facet> facets = new ArrayList<>();
        for (String field : browseBy) {
            List<Value> values = valuesResolver.apply(field);
            if (values == null) {
                continue;
            }
            facets.add(createFacet(field, labelResolver.apply(field), values));
        }
        return facets;
    }

    public static Facet createFacet(String field, String label, List<Value> values) {
        Facet facet = new Facet();
        facet.setField(field);
        facet.setLabel(label);
        facet.setValues(normalizeValues(values));
        return facet;
    }

    public static List<Value> normalizeValues(List<Value> values) {
        List<Value> normalized = values == null ? new ArrayList<>() : new ArrayList<>(values);
        normalized.sort(FACET_VALUE_ORDER);
        return normalized;
    }
}
