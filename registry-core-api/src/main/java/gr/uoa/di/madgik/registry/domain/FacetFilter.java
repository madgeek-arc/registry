package gr.uoa.di.madgik.registry.domain;

import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLDecoder;
import java.nio.charset.Charset;
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

    public static String urlDecode(String value) {
        return value != null ? URLDecoder.decode(value, Charset.defaultCharset()) : null;
    }

    // Gets all given filters
    public static Map<String, List<Object>> transform(Map<String, Object> filters) {
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

    private static Map<String, Object> createFilterFromRequestParameters(Map<String, List<Object>> parameters) {
        Map<String, Object> filtersMap = new HashMap<>();
        for (Map.Entry<String, List<Object>> filter : parameters.entrySet()) {
            filtersMap.put(filter.getKey(), filter.getValue()
                    .stream()
                    .map(encoded -> urlDecode((String) encoded))
                    .map(dirty -> dirty.replaceAll("[\\[\\]]", "")) // remove list brackets ([])
                    .map(String::trim)
                    .flatMap(values -> Arrays.stream(values.split(",")))
                    .toList());
        }
        return filtersMap;
    }

    public static FacetFilter from(MultiValueMap<String, Object> params) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(params.get("keyword") != null ? urlDecode((String) params.remove("keyword").get(0)) : "");
        ff.setFrom(params.get("from") != null ? Integer.parseInt((String) params.remove("from").get(0)) : 0);
        ff.setQuantity(params.get("quantity") != null ? Integer.parseInt((String) params.remove("quantity").get(0)) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = params.get("order") != null ? (String) params.remove("order").get(0) : "asc";
        if (!"asc".equalsIgnoreCase(orderDirection) && !"desc".equalsIgnoreCase(orderDirection))
            throw new ServiceException("Unsupported order by type");
        String orderField = params.get("orderField") != null ? (String) params.remove("orderField").get(0) : null;
        orderField = urlDecode(orderField);
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        if (params.containsKey("browseBy")) {
            ff.setBrowseBy(params.remove("browseBy")
                    .stream()
                    .map(Object::toString)
                    .map(FacetFilter::urlDecode)
                    .toList()
            );
        }
        if (!params.isEmpty()) {
            ff.setFilter(createFilterFromRequestParameters(params));
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
