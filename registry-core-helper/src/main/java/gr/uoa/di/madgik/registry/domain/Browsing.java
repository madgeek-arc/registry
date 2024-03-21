package gr.uoa.di.madgik.registry.domain;

import javax.validation.constraints.NotNull;
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
