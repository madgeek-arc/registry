package eu.openminted.registry.core.domain;

import java.util.*;

/**
 * A Filter to pass to the indexer.
 */
public class FacetFilter {
    private String keyword;

    private String resourceType;

    private int from;

    private int quantity;

    private Map<String, Object> filter;

    private List<String> browseBy;

    public FacetFilter() {
        this.filter = new HashMap<>();
        this.browseBy = new ArrayList<>();
        this.keyword = "";
        this.from = 0;
        this.quantity = 10;
    }

    public FacetFilter(List<String> browseBy) {
        this.filter = new HashMap<>();
        this.browseBy = browseBy;
        this.keyword = "";
        this.from = 0;
        this.quantity = 10;
    }

    public FacetFilter(String keyword, String resourceType, int from, int quantity, Map<String, Object> filter, List<String> browseBy) {
        this.keyword = keyword;
        this.resourceType = resourceType;
        this.from = from;
        this.quantity = quantity;
        this.filter = filter;
        this.browseBy = browseBy;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Map<String, Object> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Object> filter) {
        this.filter = filter;
    }

    public void addFilter(String key, Object value) {
        if(!Objects.equals(value,""))
            this.filter.put(key, value);
    }

    public List<String> getBrowseBy() {
        return browseBy;
    }

    public void setBrowseBy(List<String> browseBy) {
        this.browseBy = browseBy;
    }
}
