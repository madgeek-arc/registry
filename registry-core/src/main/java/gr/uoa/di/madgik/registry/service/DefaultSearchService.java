package gr.uoa.di.madgik.registry.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class DefaultSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchService.class);

    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final ObjectMapper mapper;


    public DefaultSearchService(@Qualifier("registryDataSource") DataSource dataSource) {
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
//        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

    @Override
    public Paging<Resource> cqlQuery(String query,
                                     String resourceType,
                                     int quantity,
                                     int from,
                                     String sortByField,
                                     String sortOrder) {
        validateQuantity(quantity);
        MapSqlParameterSource params = new MapSqlParameterSource();

        String q = "SELECT * FROM resource WHERE id IN (%s) OFFSET %s LIMIT %s";
        String countQuery = "SELECT COUNT(*) FROM resource WHERE id IN (%s)";
        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT(id) FROM ");
        nestedQuery.append(resourceType).append("_view ");


        if (StringUtils.hasText(query)) {
            nestedQuery.append("WHERE ");
            nestedQuery.append(translateCQLToSQL(query));
        }

        countQuery = String.format(countQuery, nestedQuery);
        Integer total = npJdbcTemplate.queryForObject(countQuery, params, new SingleColumnRowMapper<>(Integer.class));

        q = String.format(q, nestedQuery, from, quantity);

        List<Map<String, Object>> results = npJdbcTemplate.queryForList(q, params);
        List<Resource> resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).collect(Collectors.toList());
        return new Paging<>(total, from, from + quantity, resources, new ArrayList<>());
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        return cqlQuery(query, resourceType, 100, 0, "", "ASC");
    }

    @Override
    // TODO: refactor (create SQL Query Builder)
    public Paging<Resource> search(FacetFilter filter) {
        validateQuantity(filter.getQuantity());
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(filter.getKeyword())) {
            filter.setKeyword("%" + filter.getKeyword() + "%");
        } else {
            filter.setKeyword("%");
        }


        String query = "SELECT r.* FROM resource AS r INNER JOIN ( %s ) AS sv ON r.id = sv.id WHERE r.payload LIKE '%s' OFFSET %s LIMIT %s";
        String countQuery = "SELECT COUNT(*) FROM resource AS r INNER JOIN ( %s ) AS sv ON r.id = sv.id WHERE r.payload LIKE '%s'";

        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT(id) as id %s FROM ");
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
                if (entry.getValue() instanceof List) {
                    List<String> values = new ArrayList<>((List<String>) entry.getValue());
                    whereClause.append(entry.getKey()).append(" IN ")
                            .append("(")
                            .append(values.stream().map(f -> String.format("'%s'", f)).collect(Collectors.joining(",")))
                            .append(")");
                } else {
                    whereClause.append(entry.getKey()).append("=")
                            .append("'")
                            .append(entry.getValue())
                            .append("'");
                }

            } else {
                dirty = false;
            }
        }
        if (StringUtils.hasText(whereClause)) {
            nestedQuery.append("WHERE ");
            nestedQuery.append(whereClause);
        }

        List<String> orderByFields = new ArrayList<>();
        if (filter.getOrderBy() != null && !filter.getOrderBy().isEmpty()) {
            nestedQuery.append(" ORDER BY ");
            List<String> orderBy = new ArrayList<>();
            for (Map.Entry<String, Object> entry : filter.getOrderBy().entrySet()) {
                orderBy.add(String.format("%s %s", entry.getKey(), ((Map<String, Object>) entry.getValue()).get("order")));
                orderByFields.add(entry.getKey());
            }
            nestedQuery.append(String.join(",", orderBy));
        }
        String nested;
        if (orderByFields.isEmpty()) {
            nested = String.format(nestedQuery.toString(), " ");
        } else {
            nested = String.format(nestedQuery.toString(), ", " + String.join(",", orderByFields));
        }

        params.addValue("payload", filter.getKeyword());

        countQuery = String.format(countQuery, nested, filter.getKeyword());
        Integer total = npJdbcTemplate.queryForObject(countQuery, params, new SingleColumnRowMapper<>(Integer.class));

        query = String.format(query, nested, filter.getKeyword(), filter.getFrom(), filter.getQuantity());

        List<Map<String, Object>> results = npJdbcTemplate.queryForList(query, params);
        List<Resource> resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).collect(Collectors.toList());
        return new Paging<>(total, filter.getFrom(), filter.getFrom() + filter.getQuantity(), resources, new ArrayList<>());
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        String query = "SELECT * FROM resource WHERE fk_name='%s' AND payload LIKE '%%s%'";
        String countQuery = "SELECT COUNT(*) FROM resource WHERE fk_name='%s' AND payload LIKE '%%s%'";

        countQuery = String.format(countQuery, resourceType, keyword != null ? keyword : "");
        Integer total = npJdbcTemplate.queryForObject(countQuery, params, new SingleColumnRowMapper<>(Integer.class));


        query = String.format(query, resourceType, keyword != null ? keyword : "");
        List<Map<String, Object>> results = npJdbcTemplate.queryForList(query, params);

        List<Resource> resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).collect(Collectors.toList());
        return new Paging<>(total, 0, total, resources, new ArrayList<>());
    }

    @Override
    @Retryable(value = ServiceException.class, backoff = @Backoff(value = 200))
    public Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException {
        logger.debug(String.format("@Retryable 'searchId(resourceType=%s, ids={%s})'", resourceType, String.join(",", Arrays.stream(fields).map(keyValue -> keyValue.getField() + "=" + keyValue.getValue()).collect(Collectors.toSet()))));
        MapSqlParameterSource params = new MapSqlParameterSource();

        String query = "SELECT * FROM resource WHERE id IN (%s) LIMIT 1";

        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT id FROM ");
        nestedQuery.append(resourceType).append("_view ");


        StringBuilder whereClause = new StringBuilder();
        boolean dirty = false;
        for (KeyValue field : fields) {
            if (StringUtils.hasText(field.getValue())) {
                if (dirty) {
                    whereClause.append(" AND ");
                }
                dirty = true;
                params.addValue(field.getField(), field.getValue());
                whereClause.append(field.getField()).append("=")
                        .append("'")
                        .append(field.getValue())
                        .append("'");
            } else {
                dirty = false;
            }
        }
        if (StringUtils.hasText(whereClause)) {
            nestedQuery.append("WHERE ");
            nestedQuery.append(whereClause);
        }

        query = String.format(query, nestedQuery);

        Resource result = null;
        try {
            result = npJdbcTemplate.queryForObject(query, params, new DataClassRowMapper<>(Resource.class));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    public String translateCQLToSQL(String cqlQuery) {
        throw new UnsupportedOperationException("Not Implemented Yet!");
    }

    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
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
