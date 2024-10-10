package gr.uoa.di.madgik.registry.domain;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

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

    // Gets all given filters
    public static Map<String, List<Object>> getFiltersLists(FacetFilter ff) {
        Map<String, Object> filters = new LinkedHashMap<>(ff.getFilter());
        Map<String, List<Object>> allFilters = new LinkedHashMap<>();

        // fill the variable with the rest of the filters
        for (Map.Entry<String, Object> ffEntry : filters.entrySet()) {
            if (ffEntry.getValue() instanceof List) {
                allFilters.put(ffEntry.getKey(), (List) ffEntry.getValue());
            } else {
                allFilters.put(ffEntry.getKey(), Collections.singletonList(ffEntry.getValue().toString()));
            }
        }

        return allFilters;
    }

    public static FacetFilter from(Map<String, Object> params) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(params.get("keyword") != null ? (String) params.remove("keyword") : "");
        ff.setFrom(params.get("from") != null ? Integer.parseInt((String) params.remove("from")) : 0);
        ff.setQuantity(params.get("quantity") != null ? Integer.parseInt((String) params.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = params.get("order") != null ? (String) params.remove("order") : "asc";
        String orderField = params.get("orderField") != null ? (String) params.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        if (params.containsKey("browseBy")) {
            ff.setBrowseBy( (List<String>) params.remove("browseBy"));
        }
        ff.setFilter(params);
        return ff;
    }

    public static FacetFilter from(MultiValueMap<String, Object> params) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(params.get("keyword") != null ? (String) params.remove("keyword").get(0) : "");
        ff.setFrom(params.get("from") != null ? Integer.parseInt((String) params.remove("from").get(0)) : 0);
        ff.setQuantity(params.get("quantity") != null ? Integer.parseInt((String) params.remove("quantity").get(0)) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = params.get("order") != null ? (String) params.remove("order").get(0) : "asc";
        String orderField = params.get("orderField") != null ? (String) params.remove("orderField").get(0) : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        if (params.containsKey("browseBy")) {
            ff.setBrowseBy(params.remove("browseBy").stream().map(Object::toString).collect(Collectors.toList()));
        }
        if (!params.isEmpty()) {
            for (Map.Entry<String, List<Object>> filter : params.entrySet()) {
                ff.addFilter(filter.getKey(), filter.getValue());
            }
        }
        return ff;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacetFilter that = (FacetFilter) o;
        return from == that.from && quantity == that.quantity && Objects.equals(keyword, that.keyword) && Objects.equals(resourceType, that.resourceType) && Objects.equals(filter, that.filter) && Objects.equals(browseBy, that.browseBy) && Objects.equals(orderBy, that.orderBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword, resourceType, from, quantity, filter, browseBy, orderBy);
    }
}
