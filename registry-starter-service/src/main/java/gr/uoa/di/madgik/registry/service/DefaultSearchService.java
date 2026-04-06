/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.hibernate.type.SqlTypes;
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
import java.sql.Connection;
import java.sql.SQLException;
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
        mapper = new ObjectMapper().findAndRegisterModules();
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
        params.addValue("from", from);
        params.addValue("quantity", quantity);

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setOrderBy(FacetFilter.createOrderBy(List.of(sortByField), List.of(sortOrder)));

        String q = "SELECT * FROM ( %s ) ar OFFSET :from LIMIT :quantity";
        String countQuery = "SELECT COUNT(*) FROM (%s) ar";

        String nested = createQueryWithInnerJoins(filter, rt -> createViewQueryFromCqlReturningIds(query, rt));

        countQuery = String.format(countQuery, nested);
        q = String.format(q, nested);

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
        List<String> browseBy = resolveBrowseBy(filter);

        if (StringUtils.hasText(filter.getKeyword())) {
            filter.setKeyword("%" + filter.getKeyword() + "%");
        } else {
            filter.setKeyword("%");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("keyword", filter.getKeyword());
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());

        String nested = createQueryWithInnerJoins(filter, rt -> createViewQuery(filter, params, rt));
        String query = "SELECT * FROM ( %s ) ar WHERE ar.payload LIKE :keyword OFFSET :from LIMIT :quantity";
        String countQuery = "SELECT COUNT(*) FROM ( %s ) ar WHERE ar.payload LIKE :keyword";

        countQuery = String.format(countQuery, nested);
        Integer total = npJdbcTemplate.queryForObject(countQuery, params, new SingleColumnRowMapper<>(Integer.class));

        query = String.format(query, nested);
        List<Map<String, Object>> results = npJdbcTemplate.queryForList(query, params);

        List<Resource> resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).collect(Collectors.toList());
        return new Paging<>(total, filter.getFrom(), filter.getFrom() + filter.getQuantity(), resources, createFacets(browseBy, filter.getResourceType()));
    }

    @Override
    public Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) throws ServiceException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support highlighted search.");
    }

    @Override
    public List<Resource> recommend(FacetFilter filter, KeyValue idValue) throws ServiceException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support recommendations.");
    }

    private List<Facet> createFacets(List<String> browseBy, String resourceTypeName) {
        if (browseBy == null || browseBy.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, String> fieldLabels = resourceTypeService.getIndexFieldLabels(resourceTypeName);
        List<Facet> facets = new ArrayList<>();
        for (String browse : browseBy) {
            Facet facet = new Facet();
            facet.setField(browse);
            facet.setLabel(fieldLabels.get(browse));
            facet.setValues(new ArrayList<>()); // TODO: populate values
            facets.add(facet);
        }
        return facets;
    }

    private List<String> resolveBrowseBy(FacetFilter filter) {
        return SearchService.resolveBrowseBy(getResourceTypes(filter.getResourceType()), filter.getBrowseBy());
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

        MapSqlParameterSource params = new MapSqlParameterSource();

        String query = "SELECT * FROM (%s) ar LIMIT 1";
        String nested = createQueryWithInnerJoins(filter, rt -> createViewQuery(filter, params, rt));
        query = String.format(query, nested);

        Resource result = null;
        try {
            result = npJdbcTemplate.queryForObject(query, params, new ResourceRowMapper());
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
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries the {@code {resourceType}_view} directly for the requested {@code idField} /
     * {@code labelField} pairs in a single SQL statement. Both field names are validated against
     * the known {@link IndexField} names for the resource type before being interpolated into the
     * query to prevent SQL injection.
     */
    @Override
    public Map<String, String> getLabels(String resourceType, String idField,
                                         List<String> ids, String labelField) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> knownFields = resourceTypeService.getResourceTypeIndexFields(resourceType)
                .stream()
                .map(IndexField::getName)
                .collect(Collectors.toSet());

        if (!knownFields.contains(idField)) {
            throw new ServiceException(
                    String.format("Unknown idField '%s' for resource type '%s'", idField, resourceType));
        }
        if (!knownFields.contains(labelField)) {
            throw new ServiceException(
                    String.format("Unknown labelField '%s' for resource type '%s'", labelField, resourceType));
        }

        // Field names are validated above against the registry's own metadata —
        // interpolation here is intentional and safe.
        String sql = String.format(
                "SELECT %s, %s FROM %s_view WHERE %s IN (:ids)",
                idField, labelField, resourceType, idField);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(sql, params);
        Map<String, String> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object id    = row.get(idField);
            Object label = row.get(labelField);
            if (id != null && label != null) {
                result.put(id.toString(), label.toString());
            }
        }
        return result;
    }

    /**
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
     * @param filter the filter to use to get the ResourceType and generate the ODER BY clause.
     * @param innerJoinTableQueryBuilder a method returning the query to be used as a table for the inner join.
     * @return
     */
    private String createQueryWithInnerJoins(FacetFilter filter,
                                             Function<ResourceType,String> innerJoinTableQueryBuilder) {
        // "filter.getResourceType()" the resourceType name or alias
        List<ResourceType> resourceTypes = getResourceTypes(filter.getResourceType());
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
            query.append(buildOrderBy(filter));
        }
        return query.toString();
    }

    /**
     * <p>Creates a query on the view of the {@link ResourceType resourceType}, fetching the matching resources.</p>
     *
     * @param filter contains the parameters for the where clause
     * @param resourceType the {@link ResourceType} to use for the view
     * @return
     */
    private String createViewQuery(FacetFilter filter, MapSqlParameterSource params, ResourceType resourceType) {
        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT * FROM ");
        nestedQuery.append(resourceType.getName()).append("_view ");
        Set<String> knownFields = getKnownFieldNames(resourceType);

        StringBuilder whereClause = new StringBuilder();
        boolean dirty = false;
        for (Map.Entry<String, Object> entry : filter.getFilter().entrySet()) {
            String field = sanitizeFieldName(entry.getKey());
            if (entry.getValue() == null || !knownFields.contains(field)) {
                continue;
            }

            if (dirty) {
                whereClause.append(" AND ");
            }
            dirty = true;

            List<Object> filterValues = transformFilterValuesType(resourceType, field, entry.getValue());
            params.addValue(field, unwrapListWhenSingle(filterValues));

            // append where clause
            if (isDataTypeArray(resourceType, field)) {
                // PostgreSQL specific code: Checks whether the array contains any occurrence of the values list
                Connection conn;
                try {
                    conn = Objects.requireNonNull(npJdbcTemplate.getJdbcTemplate().getDataSource()).getConnection();
                    params.addValue(field, conn.createArrayOf("text", filterValues.toArray()), SqlTypes.ARRAY); // replace existing value with correct one
                } catch (SQLException e) {
                    logger.error("Failed to execute SQL operation for entry: {} with values: {}. Error: {}", field, filterValues.toArray(), e.getMessage(), e);
                }
                whereClause.append(String.format("%s && :%s", field, field));
            } else if (filterValues.size() != 1) {
                whereClause.append(String.format("%s IN (:%s)", field, field));
            } else {
                whereClause.append(String.format("%s = :%s", field, field));
            }
        }
        for (Map.Entry<String, RangeFilter> entry : filter.getRangeFilters().entrySet()) {
            String field = sanitizeFieldName(entry.getKey());
            if (!knownFields.contains(field)) continue;
            RangeFilter rf = entry.getValue();

            List<String> conditions = new ArrayList<>();
            if (rf.getFrom() != null) {
                params.addValue(field + "_from", rf.getFrom());
                conditions.add(String.format("%s >= :%s_from", field, field));
            }
            if (rf.getTo() != null) {
                params.addValue(field + "_to", rf.getTo());
                conditions.add(String.format("%s <= :%s_to", field, field));
            }

            if (conditions.isEmpty()) {
                // Both bounds null: emit "IS NULL" only when caller wants null records to pass
                if (rf.isIncludeNull()) {
                    if (dirty) whereClause.append(" AND ");
                    dirty = true;
                    whereClause.append(String.format("(%s IS NULL)", field));
                }
                // includeNull=false + no bounds → no constraint, nothing to add
                continue;
            }

            if (dirty) {
                whereClause.append(" AND ");
            }
            dirty = true;
            String rangeExpr = String.join(" AND ", conditions);
            if (rf.isIncludeNull()) {
                // Records where the field is NULL also pass (e.g. null expiryDate = never expires)
                whereClause.append(String.format("(%s IS NULL OR (%s))", field, rangeExpr));
            } else {
                whereClause.append(String.format("(%s)", rangeExpr));
            }
        }

        if (StringUtils.hasText(whereClause)) {
            nestedQuery.append("WHERE ");
            nestedQuery.append(whereClause);
        }

        return nestedQuery.toString();
    }

    private static String buildOrderBy(FacetFilter filter) {
        StringBuilder orderBuilder = new StringBuilder();
        if (filter.getOrderBy() != null && !filter.getOrderBy().isEmpty()) {
            orderBuilder.append(" ORDER BY ");
            List<String> orderBy = new ArrayList<>();
            for (Map.Entry<String, Object> entry : filter.getOrderBy().entrySet()) {
                String field = entry.getKey().replaceAll("[^A-Za-z0-9_]","");
                String order = (String) ((Map<String, Object>) entry.getValue()).get("order");
                orderBy.add(String.format("%s %s", field, "desc".equalsIgnoreCase(order) ? "DESC" : "ASC"));
            }
            orderBuilder.append(String.join(",", orderBy));
        }
        return orderBuilder.toString();
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
        nestedQuery.append("SELECT DISTINCT * FROM ");
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

    /**
     * Resolves whether the given indexed field is multivalued for the provided resource type.
     *
     * @param resourceType the resource type owning the indexed fields
     * @param columnName the validated field name to inspect
     * @return {@code true} when the field is stored as an array-backed column
     */
    private boolean isDataTypeArray(ResourceType resourceType, String columnName) {
        IndexField field = resourceType.getIndexFields().stream()
                .filter(rt -> rt.getName().equals(columnName))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Could not find field"));
        return field.isMultivalued();
    }

    /**
     * Extracts the indexed field names declared for the resource type.
     *
     * @param resourceType the resource type whose searchable fields should be returned
     * @return the set of known indexed field names, or an empty set when none are defined
     */
    private Set<String> getKnownFieldNames(ResourceType resourceType) {
        if (resourceType.getIndexFields() == null) {
            return Collections.emptySet();
        }
        return resourceType.getIndexFields().stream()
                .map(IndexField::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Normalizes a user-provided field name before it is interpolated into SQL.
     *
     * <p>Only alphanumeric characters and underscores are retained. Callers are still expected
     * to validate the sanitized result against the resource type metadata.</p>
     *
     * @param field the raw field name supplied by the caller
     * @return the sanitized field name, or an empty string when nothing valid remains
     */
    private String sanitizeFieldName(String field) {
        if (field == null) {
            return "";
        }
        return field.replaceAll("[^A-Za-z0-9_]", "");
    }

    static private class ResourcePropertyName extends PropertyNamingStrategies.NamingBase {

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

    /**
     * Coerces filter values to the Java type declared by the indexed field metadata.
     *
     * @param resourceType the resource type containing the indexed field definition
     * @param fieldName the field name whose type should be applied
     * @param value the raw filter value or collection of values
     * @return the normalized values ready to be bound as SQL parameters
     */
    private static List<Object> transformFilterValuesType(ResourceType resourceType, String fieldName, Object value) {
        List<Object> valuesList = new ArrayList<>();
        if (Collection.class.isAssignableFrom(value.getClass())) {
            valuesList.addAll((Collection<?>) value);
        } else {
            valuesList.add(value);
        }

        if (resourceType.getIndexFields() != null) {
            String type = resourceType.getIndexFields().stream().filter(i -> i.getName().equals(fieldName)).findFirst().get().getType();
            if (Boolean.class.getName().equals(type)) {
                valuesList = (List) valuesList.stream().map(v -> Boolean.parseBoolean(String.valueOf(v))).toList();
            }
            // add more statements if required (e.g. parse Integer)
        }
        return valuesList;
    }

    /**
     * Unwraps the List if it contains only one item, otherwise it returns the list as is.
     * @param values the list
     * @return the list or the single value
     */
    private static Object unwrapListWhenSingle(List<Object> values) {
        if (values.size() == 1) {
            return values.get(0);
        }
        return values;
    }
}
