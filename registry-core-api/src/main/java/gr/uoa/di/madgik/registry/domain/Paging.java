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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Paging<T> {

    private int total;

    private int from;

    private int to;

    private List<T> results;

    private List<Facet> facets;

    public Paging(int total, int from, int to, List<T> results, List<Facet> facets) {
        this.total = total;
        this.from = from;
        this.to = to;
        this.results = results;
        this.facets = facets;
    }

    public Paging(@NotNull Paging<T> page) {
        this.total = page.getTotal();
        this.from = page.getFrom();
        this.to = page.getTo();
        this.results = page.getResults();
        this.facets = page.getFacets();
    }

    public <K> Paging(@NotNull Paging<K> page, List<T> results) {
        this.total = page.getTotal();
        this.from = page.getFrom();
        this.to = page.getTo();
        this.facets = page.getFacets();
        this.results = results;
    }

    public Paging() {
        this.total = 0;
        this.from = 0;
        this.to = 0;
        this.results = new ArrayList<>();
        this.facets = new ArrayList<>();
    }

    public <U> Paging<U> map(Function<? super T, ? extends U> converter) {
        return new Paging<>(this, this.getResults().stream().map(converter).collect(Collectors.toList()));
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }
}
