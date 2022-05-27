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

    private Map<String, Object> orderBy;

    public FacetFilter() {
        this.filter = new HashMap<>();
        this.browseBy = new ArrayList<>();
        this.keyword = "";
        this.from = 0;
        this.quantity = 10;
        this.orderBy = null;
    }

    public FacetFilter(List<String> browseBy) {
        this.filter = new HashMap<>();
        this.browseBy = browseBy;
        this.keyword = "";
        this.from = 0;
        this.quantity = 10;
        this.orderBy = null;
    }

    public FacetFilter(String keyword, String resourceType, int from, int quantity,
                       Map<String, Object> filter, List<String> browseBy,
                       Map<String, Object> orderBy) {
        this.keyword = keyword;
        this.resourceType = resourceType;
        this.from = from;
        this.quantity = quantity;
        this.filter = filter;
        this.browseBy = browseBy;
        this.orderBy = orderBy;
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
        if (!Objects.equals(value, ""))
            this.filter.put(key, value);
    }

    public List<String> getBrowseBy() {
        return browseBy;
    }

    public void setBrowseBy(List<String> browseBy) {
        this.browseBy = browseBy;
    }

    public Map<String, Object> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(Map<String, Object> orderBy) {
        this.orderBy = orderBy;
    }

    public void addOrderBy(String field, String order) {
        if (orderBy == null) {
            this.orderBy = new HashMap<>();
        }
        Map<String, Object> orderType = new HashMap<>();
        orderType.put("order", order);
        orderBy.put(field, orderType);
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof FacetFilter)) return false;
        if (!super.equals(object)) return false;
        FacetFilter that = (FacetFilter) object;
        return from == that.from && quantity == that.quantity && java.util.Objects.equals(keyword, that.keyword) && java.util.Objects.equals(resourceType, that.resourceType) && java.util.Objects.equals(filter, that.filter) && java.util.Objects.equals(browseBy, that.browseBy) && java.util.Objects.equals(orderBy, that.orderBy);
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), keyword, resourceType, from, quantity, filter, browseBy, orderBy);
    }
}
