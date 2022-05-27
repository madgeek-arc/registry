package eu.openminted.registry.core.domain;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Paging)) return false;
        if (!super.equals(object)) return false;
        Paging<?> paging = (Paging<?>) object;
        return total == paging.total && from == paging.from && to == paging.to && java.util.Objects.equals(results, paging.results) && java.util.Objects.equals(facets, paging.facets);
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), total, from, to, results, facets);
    }
}
