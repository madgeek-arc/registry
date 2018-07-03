package eu.openminted.registry.core.domain;

import java.util.ArrayList;
import java.util.List;

public class Paging<T> {
    int total;
    int from;
    int to;
    List<T> results;
    Occurrences occurrences;

    public Paging(int total, int from, int to, List<T> results, Occurrences occurrences) {
        this.total = total;
        this.from = from;
        this.to = to;
        this.results = results;
        this.occurrences = occurrences;
    }

    public Paging() {
        this.total = 0;
        this.from = 0;
        this.to = 0;
        this.results = new ArrayList<>();
        this.occurrences = new Occurrences();
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

    public Occurrences getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(Occurrences occurrences) {
        this.occurrences = occurrences;
    }

}
