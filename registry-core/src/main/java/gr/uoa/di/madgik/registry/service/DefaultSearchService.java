package gr.uoa.di.madgik.registry.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.querydsl.core.Query;
import com.querydsl.core.QueryFactory;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchService.class);

    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final ObjectMapper mapper;
    @Value("${elastic.aggregation.topHitsSize:100}")
    private int topHitsSize;
    @Value("${elastic.aggregation.bucketSize:100}")
    private int bucketSize;
    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public DefaultSearchService(@Qualifier("registryDataSource") DataSource dataSource) {
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
//        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

    @Override
    public Paging<Resource> query(FacetFilter filter) {
        validateQuantity(filter.getQuantity());
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Paging<Resource> query(String query,
                                  String resourceType,
                                  int quantity,
                                  int from,
                                  String sortByField,
                                  String sortOrder) {
        validateQuantity(quantity);
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Paging<Resource> query(String query, String resourceType) {
        return query(query, resourceType, 100, 0, "", "ASC");
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        String query = "SELECT * FROM resource WHERE id IN (%s)";
        String countQuery = "SELECT COUNT(*) FROM resource WHERE id IN (%s)";
        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT(id) FROM ");
        nestedQuery.append(filter.getResourceType()).append("_view ");


        StringBuilder whereClause = new StringBuilder();
        boolean dirty = false;
        for (Map.Entry<String, Object> entry : filter.getFilter().entrySet()) {
            if (entry.getValue() != null) {
                if (dirty) {
                    whereClause.append(" AND ");
                }
                dirty = true;
                params.addValue(entry.getKey(), entry.getValue());
                whereClause.append(entry.getKey()).append("=")
                        .append("'")
                        .append(entry.getValue())
                        .append("'");
            } else { // TODO: create logic when value is a composite
                dirty = false;
            }
        }
        if (StringUtils.hasText(whereClause)) {
            nestedQuery.append("WHERE ");
            nestedQuery.append(whereClause);
        }

        // TODO: implement ORDER BY
//        nestedQuery.append(" ORDER BY ").append()).append();

        countQuery = String.format(countQuery, nestedQuery);
        Integer total = npJdbcTemplate.queryForObject(countQuery, params, new SingleColumnRowMapper<>(Integer.class));

        nestedQuery.append(" OFFSET ").append(filter.getFrom());
        nestedQuery.append(" LIMIT ").append(filter.getQuantity());

        query = String.format(query, nestedQuery);

        List<Map<String, Object>> results = npJdbcTemplate.queryForList(query, params);
        List<Resource> resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).collect(Collectors.toList());
        return new Paging<>(total, filter.getFrom(), filter.getFrom() + filter.getQuantity(), resources, new ArrayList<>());
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    @Retryable(value = ServiceException.class, backoff = @Backoff(value = 200))
    public Resource searchId(String resourceType, KeyValue... ids) throws ServiceException {
        logger.debug(String.format("@Retryable 'searchId(resourceType=%s, ids={%s})'", resourceType, String.join(",", Arrays.stream(ids).map(keyValue -> keyValue.getField() + "=" + keyValue.getValue()).collect(Collectors.toSet()))));
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    private void validateQuantity(int quantity) {
        if (quantity > maxQuantity) {
            throw new IllegalArgumentException(String.format("Quantity should be up to %s.", maxQuantity));
        } else if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    private String buildQuery(FacetFilter facetFilter) {
        String query = "SELECT * FROM resource WHERE id IN (%s)";
        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT(id) FROM ");
        nestedQuery.append(facetFilter.getResourceType()).append("_view ");
        nestedQuery.append("WHERE ");

        StringBuilder whereClause = new StringBuilder();
        boolean dirty = false;
        for (Map.Entry<String, Object> entry : facetFilter.getFilter().entrySet()) {
            if (entry.getValue() instanceof String) { // only resolves string values
                if (dirty) {
                    whereClause.append(" AND ");
                }
                dirty = true;
                whereClause.append(entry.getKey()).append("=").append(entry.getValue());
            } else { // TODO: create logic when value is a composite
                dirty = false;
            }
        }
        nestedQuery.append(whereClause);
//        nestedQuery.append(" ORDER BY ").append()).append();

        return String.format(query, nestedQuery);
    }

    static private class ResourcePropertyName extends PropertyNamingStrategy.PropertyNamingStrategyBase {

        @Override
        public String translate(String propertyName) {
            switch (propertyName) {
                case "modificationDate":
                    return "modification_date";
                case "creationDate":
                    return "creation_date";
                case "resourceTypeName":
                    return "fk_name";
                case "payloadFormat":
                    return "payloadformat";
                default:
                    return propertyName;
            }
        }
    }
}
