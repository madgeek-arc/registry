package gr.uoa.di.madgik.registry.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class DefaultSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchService.class);

    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final ObjectMapper mapper;
    private final ResourceTypeService resourceTypeService;

    public DefaultSearchService(@Qualifier("registryDataSource") DataSource dataSource,
                                ResourceTypeService resourceTypeService) {
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
//        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        this.resourceTypeService = resourceTypeService;
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

        String nested = createQueryWithInnerJoinsReturningIds(resourceType, rt -> createViewQueryFromCqlReturningIds(query, rt));

        countQuery = String.format(countQuery, nested);
        q = String.format(q, nested, from, quantity);

        Integer total = 0;
        List<Resource> resources;

        total = npJdbcTemplate.queryForObject(countQuery, params, new SingleColumnRowMapper<>(Integer.class));
        List<Map<String, Object>> results = npJdbcTemplate.queryForList(q, params);
        resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).collect(Collectors.toList());

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

        if (StringUtils.hasText(filter.getKeyword())) {
            filter.setKeyword("%" + filter.getKeyword() + "%");
        } else {
            filter.setKeyword("%");
        }

        String nested = createQueryWithInnerJoins(filter.getResourceType(), rt -> createViewQueryReturningIds(filter, rt));
        String query = "SELECT * FROM ( %s ) ar WHERE ar.payload LIKE '%s' OFFSET %s LIMIT %s";
        String countQuery = "SELECT COUNT(*) FROM ( %s ) ar WHERE ar.payload LIKE '%s'";

        countQuery = String.format(countQuery, nested, filter.getKeyword());
        Integer total = npJdbcTemplate.queryForObject(countQuery, new HashMap<>(), new SingleColumnRowMapper<>(Integer.class));

        query = String.format(query, nested, filter.getKeyword(), filter.getFrom(), filter.getQuantity());
        List<Map<String, Object>> results = npJdbcTemplate.queryForList(query, new HashMap<>());

        List<Resource> resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).collect(Collectors.toList());
        return new Paging<>(total, filter.getFrom(), filter.getFrom() + filter.getQuantity(), resources, createFacets(filter.getBrowseBy()));
    }

    private List<Facet> createFacets(List<String> browseBy) {
        List<Facet> facets = new ArrayList<>();
        if (browseBy != null && !browseBy.isEmpty()) {
            for (String browse : browseBy) {
                Facet facet = new Facet();
                facet.setField(browse);
                facet.setLabel(Arrays.stream(browse.split("_"))
                        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                        .collect(Collectors.joining(" ")));
                List<Value> values = new ArrayList<>(); // TODO: populate values
                facet.setValues(values);
                facets.add(facet);
            }
        }
        return facets;
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setKeyword(keyword);
        filter.setQuantity(Integer.MAX_VALUE);
        return search(filter);
    }

    @Override
    @Retryable(value = ServiceException.class, backoff = @Backoff(value = 200))
    public Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException {
        logger.debug(String.format("@Retryable 'searchId(resourceType=%s, ids={%s})'", resourceType, String.join(",", Arrays.stream(fields).map(keyValue -> keyValue.getField() + "=" + keyValue.getValue()).collect(Collectors.toSet()))));

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setFrom(0);
        filter.setQuantity(1);
        for (KeyValue keyValue : fields) {
            filter.addFilter(keyValue.getField(), keyValue.getValue());
        }

        String query = "SELECT * FROM resource WHERE id IN (%s) LIMIT 1";
        String nested = createQueryWithInnerJoinsReturningIds(resourceType, rt -> createViewQueryReturningIds(filter, rt));
        query = String.format(query, nested);

        Resource result = null;
        try {
            result = npJdbcTemplate.queryForObject(query, new HashMap<>(), new ResourceRowMapper());
        } catch (EmptyResultDataAccessException ignore) {
            return null; // when no result is found
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }/**
     * Get a list of ResourceTypes based on the provided resourceType name or alias.
     *
     * @param resourceTypeOrAlias the name of the resourceType or an alias
     * @return
     */
    private List<ResourceType> getResourceTypes(String resourceTypeOrAlias) {
        List<ResourceType> resourceTypes = new ArrayList<>();

        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeOrAlias);
        if (resourceType == null) {
            resourceTypes = resourceTypeService.getAllResourceTypeByAlias(resourceTypeOrAlias);
            if (resourceTypes.isEmpty()) {
                throw new ServiceException("No resource types found for alias: " + resourceTypeOrAlias);
            }
        } else resourceTypes.add(resourceType);
        return resourceTypes;
    }

    /**
     * <p>Creates a query returning all matching {@link Resource resources}.</p>
     * <p>In case the given resourceType is an alias,
     * it creates multiple queries combined with unions. </p>
     *
     * @param resourceTypeOrAlias the resourceType name or alias
     * @param innerJoinTableQueryBuilder a method returning the query to be used as a table for the inner join
     * @return
     */
    private String createQueryWithInnerJoins(String resourceTypeOrAlias, Function<ResourceType,String> innerJoinTableQueryBuilder) {
        List<ResourceType> resourceTypes = getResourceTypes(resourceTypeOrAlias);
        final String outerJoinTemplate = "SELECT r.* FROM resource AS r INNER JOIN ( %s ) AS v%d ON r.id = v%d.id ";
        StringBuilder query = new StringBuilder();

        if (resourceTypes == null || resourceTypes.isEmpty()) {
            logger.error("No resource types found");
            return "";
        } else {
            for (int i = 0; i < resourceTypes.size(); i++) {
                query.append(String.format(outerJoinTemplate, innerJoinTableQueryBuilder.apply(resourceTypes.get(i)), i, i));
                if (i != resourceTypes.size() - 1) {
                    query.append(" UNION ALL ");
                }
            }
        }
        return query.toString();
    }


    /**
     * <p>Creates a query returning all matching {@link Resource resources}.</p>
     * <p>In case the given resourceType is an alias,
     * it creates multiple queries combined with unions. </p>
     *
     * @param resourceTypeOrAlias the resourceType name or alias
     * @param innerJoinTableQueryBuilder a method returning the query to be used as a table for the inner join
     * @return
     */
    private String createQueryWithInnerJoinsReturningIds(String resourceTypeOrAlias, Function<ResourceType,String> innerJoinTableQueryBuilder) {
        List<ResourceType> resourceTypes = getResourceTypes(resourceTypeOrAlias);
        final String outerJoinTemplate = "SELECT r.id FROM resource AS r INNER JOIN ( %s ) AS v%d ON r.id = v%d.id ";
        StringBuilder query = new StringBuilder();

        if (resourceTypes == null || resourceTypes.isEmpty()) {
            logger.error("No resource types found");
            return "";
        } else {
            for (int i = 0; i < resourceTypes.size(); i++) {
                query.append(String.format(outerJoinTemplate, innerJoinTableQueryBuilder.apply(resourceTypes.get(i)), i, i));
                if (i != resourceTypes.size() - 1) {
                    query.append(" UNION ALL ");
                }
            }
        }
        return query.toString();
    }

    /**
     * Creates a query on the view of the {@link ResourceType resourceType}, fetching the IDs of the matching resources.
     *
     * @param filter contains the parameters for the where clause and the order by clause
     * @param resourceType the {@link ResourceType} to use for the view
     * @return
     */
    private String createViewQueryReturningIds(FacetFilter filter, ResourceType resourceType) {
        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT(id) as id %s FROM ");
        nestedQuery.append(resourceType.getName()).append("_view ");

        StringBuilder whereClause = new StringBuilder();
        boolean dirty = false;
        for (Map.Entry<String, Object> entry : filter.getFilter().entrySet()) {
            if (entry.getValue() != null) {
                if (dirty) {
                    whereClause.append(" AND ");
                }
                dirty = true;

                // format values as string
                String valuesToString;
                if (entry.getValue() instanceof List) {
                    List<String> values = new ArrayList<>((List<String>) entry.getValue());
                    valuesToString = values.stream().map(f -> String.format("'%s'", f)).collect(Collectors.joining(","));
                } else {
                    valuesToString = String.format("'%s'", entry.getValue().toString());
                }

                // append where clause
                if (isDataTypeArray(resourceType.getName(), entry.getKey())) {
                    valuesToString = valuesToString.replaceAll("'", "\"");
                    // search for any occurrence
                    whereClause.append(entry.getKey()).append(String.format(" && '{%s}'", valuesToString));
                } else {
                    whereClause.append(entry.getKey()).append(String.format(" IN (%s)", valuesToString));
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

        return nested;
    }

    /**
     * Creates an SQL query on the 'id' field using the given cql query as the WHERE clause.
     *
     * @param cqlQuery the cql query to use for the WHERE clause
     * @param resourceType the resourceType to use for the view name
     * @return
     */
    private String createViewQueryFromCqlReturningIds(String cqlQuery, ResourceType resourceType) {
        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT(id) FROM ");
        nestedQuery.append(resourceType.getName()).append("_view ");


        if (StringUtils.hasText(cqlQuery)) {
            nestedQuery.append("WHERE ");
            nestedQuery.append(translateCQLToSQL(cqlQuery));
        }
        return nestedQuery.toString();
    }

    /**
     * Translates a CQL query to the WHERE clause of an SQL query.
     *
     * @param cqlQuery the query to translate
     * @return a valid SQL WHERE clause
     */
    public String translateCQLToSQL(String cqlQuery) {
        if (cqlQuery.contains(";")) {
            logger.warn("Possible SQL Injection attempt: query='{}'", cqlQuery);
            throw new IllegalArgumentException("Found terminating character ';' in cql query");
        }
        // TODO: Translate properly
        return cqlQuery.replaceAll("\"", "'");
    }

    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    private boolean isDataTypeArray(String resourceTypeName, String columnName) {
        IndexField field = resourceTypeService.getResourceTypeIndexFields(resourceTypeName).stream().filter(rt -> rt.getName().equals(columnName)).findFirst().orElseThrow(() -> new ServiceException("Could not find field"));
        return field.isMultivalued();
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
