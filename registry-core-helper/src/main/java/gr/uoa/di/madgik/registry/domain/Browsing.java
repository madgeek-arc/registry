/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class Browsing<T> extends Paging<T> {

    public Browsing() {

    }

    public Browsing(int total, int from, int to, List<T> results, List<Facet> facets) {
        super(total, from, to, results, facets);
    }

    public Browsing(Browsing b, List<T> results, List<Facet> facets) {
        super(b.getTotal(), b.getFrom(), b.getTo(), results, facets);
    }

    public <K> Browsing(Paging<K> paging, List<T> results, Map<String, String> labels) {
        super(paging, results);
        createFacetCollection(labels);
    }

    public void createFacetCollection(@NotNull Map<String, String> labels) {
        assert getFacets() != null;
        getFacets().forEach(x -> x.setLabel(labels.get(x.getField())));
    }
}
