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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.RangeFilter;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.hibernate.type.SqlTypes;
import org.xbib.cql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builder for the SQL statements used by {@link DefaultSearchService}.
 *
 * <p>The builder owns SQL assembly and parameter binding, while the service stays responsible
 * for executing the resulting statements and mapping rows.</p>
 */
final class SearchSqlQueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SearchSqlQueryBuilder.class);

    private final DataSource dataSource;
    private int cqlParameterIndex = 0;

    private SearchSqlQueryBuilder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    static Builder builder(DataSource dataSource) {
        return new Builder(new SearchSqlQueryBuilder(dataSource));
    }

    static final class Builder {

        private final SearchSqlQueryBuilder delegate;
        private FacetFilter filter;
        private List<ResourceType> resourceTypes = Collections.emptyList();
        private MapSqlParameterSource params = new MapSqlParameterSource();
        private String cqlQuery;

        private Builder(SearchSqlQueryBuilder delegate) {
            this.delegate = delegate;
        }

        Builder withFilter(FacetFilter filter) {
            this.filter = filter;
            return this;
        }

        Builder withResourceTypes(List<ResourceType> resourceTypes) {
            this.resourceTypes = resourceTypes;
            return this;
        }

        Builder withParameters(MapSqlParameterSource params) {
            this.params = params;
            return this;
        }

        Builder withCqlQuery(String cqlQuery) {
            this.cqlQuery = cqlQuery;
            return this;
        }

        SearchSqlQuery buildSearchQuery() {
            String nestedQuery = delegate.createQueryWithInnerJoins(filter, resourceTypes,
                    resourceType -> delegate.createViewQuery(filter, params, resourceType));
            return new SearchSqlQuery(
                    nestedQuery,
                    "SELECT * FROM ( %s ) ar WHERE ar.payload LIKE :keyword OFFSET :from LIMIT :quantity".formatted(nestedQuery),
                    "SELECT COUNT(*) FROM ( %s ) ar WHERE ar.payload LIKE :keyword".formatted(nestedQuery),
                    params
            );
        }

        SearchSqlQuery buildSingleResultQuery() {
            String nestedQuery = delegate.createQueryWithInnerJoins(filter, resourceTypes,
                    resourceType -> delegate.createViewQuery(filter, params, resourceType));
            return new SearchSqlQuery(
                    nestedQuery,
                    "SELECT * FROM (%s) ar LIMIT 1".formatted(nestedQuery),
                    null,
                    params
            );
        }

        SearchSqlQuery buildCqlQuery() {
            String nestedQuery = delegate.createQueryWithInnerJoins(filter, resourceTypes,
                    resourceType -> delegate.createViewQueryFromCqlReturningIds(cqlQuery, resourceType, params));
            return new SearchSqlQuery(
                    nestedQuery,
                    "SELECT * FROM ( %s ) ar OFFSET :from LIMIT :quantity".formatted(nestedQuery),
                    "SELECT COUNT(*) FROM (%s) ar".formatted(nestedQuery),
                    params
            );
        }
    }

    static final class SearchSqlQuery {

        private final String nestedQuery;
        private final String resultQuery;
        private final String countQuery;
        private final MapSqlParameterSource params;

        private SearchSqlQuery(String nestedQuery, String resultQuery, String countQuery,
                               MapSqlParameterSource params) {
            this.nestedQuery = nestedQuery;
            this.resultQuery = resultQuery;
            this.countQuery = countQuery;
            this.params = params;
        }

        String nestedQuery() {
            return nestedQuery;
        }

        String resultQuery() {
            return resultQuery;
        }

        String countQuery() {
            return countQuery;
        }

        MapSqlParameterSource params() {
            return params;
        }
    }

    /**
     * <p>Creates a query returning all matching {@link gr.uoa.di.madgik.registry.domain.Resource resources}
     * including alias expansion and ordering.</p>
     *
     * @param filter the filter to use to get the ResourceType and generate the ODER BY clause.
     * @param innerJoinTableQueryBuilder a method returning the query to be used as a table for the inner join.
     * @return
     */
    private String createQueryWithInnerJoins(FacetFilter filter,
                                             List<ResourceType> resourceTypes,
                                             Function<ResourceType, String> innerJoinTableQueryBuilder) {
        final String outerJoinTemplate = "SELECT r.* FROM resource AS r INNER JOIN ( %s ) AS v%d ON r.id = v%d.id ";
        StringBuilder query = new StringBuilder();

        if (resourceTypes == null || resourceTypes.isEmpty()) {
            logger.error("No resource types found");
            return "";
        }

        for (int i = 0; i < resourceTypes.size(); i++) {
            query.append(String.format(outerJoinTemplate, innerJoinTableQueryBuilder.apply(resourceTypes.get(i)), i, i));
            if (i != resourceTypes.size() - 1) {
                query.append(" UNION ALL ");
            }
        }
        query.append(buildOrderBy(filter));
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

            if (isDataTypeArray(resourceType, field)) {
                bindPostgresArray(params, field, filterValues);
                whereClause.append(String.format("%s && :%s", field, field));
            } else if (filterValues.size() != 1) {
                whereClause.append(String.format("%s IN (:%s)", field, field));
            } else {
                whereClause.append(String.format("%s = :%s", field, field));
            }
        }

        for (Map.Entry<String, RangeFilter> entry : filter.getRangeFilters().entrySet()) {
            String field = sanitizeFieldName(entry.getKey());
            if (!knownFields.contains(field)) {
                continue;
            }
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
                if (rf.isIncludeNull()) {
                    if (dirty) {
                        whereClause.append(" AND ");
                    }
                    dirty = true;
                    whereClause.append(String.format("(%s IS NULL)", field));
                }
                continue;
            }

            if (dirty) {
                whereClause.append(" AND ");
            }
            dirty = true;
            String rangeExpr = String.join(" AND ", conditions);
            if (rf.isIncludeNull()) {
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

    /**
     * Creates the view query used by CQL search after translating the caller-supplied expression.
     *
     * @param cqlQuery the cql query to use for the WHERE clause
     * @param resourceType the resourceType to use for the view name
     * @return
     */
    private String createViewQueryFromCqlReturningIds(String cqlQuery, ResourceType resourceType,
                                                      MapSqlParameterSource params) {
        StringBuilder nestedQuery = new StringBuilder();
        nestedQuery.append("SELECT DISTINCT * FROM ");
        nestedQuery.append(resourceType.getName()).append("_view ");

        if (StringUtils.hasText(cqlQuery)) {
            nestedQuery.append("WHERE ");
            nestedQuery.append(translateCqlToSql(cqlQuery, resourceType, params));
        }
        return nestedQuery.toString();
    }

    /**
     * Translates a CQL query to the WHERE clause currently supported by the SQL backend.
     *
     * @param cqlQuery the query to translate
     * @return a valid SQL WHERE clause
     */
    private String translateCqlToSql(String cqlQuery, ResourceType resourceType, MapSqlParameterSource params) {
        if (cqlQuery.contains(";")) {
            logger.warn("Possible SQL Injection attempt: query='{}'", cqlQuery);
            throw new IllegalArgumentException("Found terminating character ';' in cql query");
        }
        try {
            CQLParser parser = new CQLParser(cqlQuery);
            parser.parse();
            return new CqlSqlQueryGenerator(resourceType, params).toSql(parser.getCQLQuery());
        } catch (ServiceException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CQL query", e);
        }
    }

    private void bindPostgresArray(MapSqlParameterSource params, String field, List<Object> filterValues) {
        try (Connection conn = Objects.requireNonNull(dataSource).getConnection()) {
            params.addValue(field, conn.createArrayOf("text", filterValues.toArray()), SqlTypes.ARRAY);
        } catch (SQLException e) {
            throw new ServiceException("Failed to bind array parameter '%s'".formatted(field), e);
        }
    }

    private static String buildOrderBy(FacetFilter filter) {
        StringBuilder orderBuilder = new StringBuilder();
        if (filter.getOrderBy() != null && !filter.getOrderBy().isEmpty()) {
            orderBuilder.append(" ORDER BY ");
            List<String> orderBy = new ArrayList<>();
            for (Map.Entry<String, Object> entry : filter.getOrderBy().entrySet()) {
                String field = entry.getKey().replaceAll("[^A-Za-z0-9_]", "");
                String order = (String) ((Map<String, Object>) entry.getValue()).get("order");
                orderBy.add(String.format("%s %s", field, "desc".equalsIgnoreCase(order) ? "DESC" : "ASC"));
            }
            orderBuilder.append(String.join(",", orderBy));
        }
        return orderBuilder.toString();
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
            String type = resourceType.getIndexFields().stream()
                    .filter(i -> i.getName().equals(fieldName))
                    .findFirst()
                    .get()
                    .getType();
            switch (type) {
                case "java.lang.Boolean" -> valuesList = valuesList.stream()
                        .map(v -> Boolean.parseBoolean(String.valueOf(v)))
                        .collect(Collectors.toCollection(ArrayList::new));
                case "java.lang.Integer", "java.lang.Long" -> valuesList = valuesList.stream()
                        .map(v -> Long.parseLong(String.valueOf(v)))
                        .collect(Collectors.toCollection(ArrayList::new));
                case "java.lang.Float" -> valuesList = valuesList.stream()
                        .map(v -> Double.parseDouble(String.valueOf(v)))
                        .collect(Collectors.toCollection(ArrayList::new));
                case "java.time.Instant" -> valuesList = valuesList.stream()
                        .map(v -> coerceInstant(String.valueOf(v)))
                        .collect(Collectors.toCollection(ArrayList::new));
                default -> {
                    // do nothing
                }
            }
        }
        return valuesList;
    }

    private static Object coerceInstant(String value) {
        try {
            return Timestamp.from(Instant.parse(value));
        } catch (Exception _) {
            try {
                return Timestamp.from(Instant.ofEpochMilli(Long.parseLong(value)));
            } catch (Exception _) {
                throw new IllegalArgumentException("Invalid instant value: " + value);
            }
        }
    }

    private static String normalizeTermValue(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Unwraps the List if it contains only one item, otherwise it returns the list as is.
     * @param values the list
     * @return the list or the single value
     */
    private static Object unwrapListWhenSingle(List<Object> values) {
        return values.size() == 1 ? values.getFirst() : values;
    }

    private final class CqlSqlQueryGenerator implements Visitor {

        private final ResourceType resourceType;
        private final MapSqlParameterSource params;
        private final Set<String> knownFields;
        private final Deque<String> fragments = new ArrayDeque<>();

        private CqlSqlQueryGenerator(ResourceType resourceType, MapSqlParameterSource params) {
            this.resourceType = resourceType;
            this.params = params;
            this.knownFields = getKnownFieldNames(resourceType);
        }

        private String toSql(SortedQuery query) {
            if (query == null) {
                throw new IllegalArgumentException("Invalid CQL query");
            }
            query.accept(this);
            if (fragments.size() != 1) {
                throw new IllegalArgumentException("Invalid CQL query");
            }
            return fragments.pop();
        }

        @Override
        public void visit(SortedQuery node) {
            if (node.getSortSpec() != null) {
                throw new IllegalArgumentException("CQL sort clauses are not supported");
            }
            node.getQuery().accept(this);
        }

        @Override
        public void visit(Query node) {
            if (node.getPrefixAssignments() != null && !node.getPrefixAssignments().isEmpty()) {
                throw new IllegalArgumentException("CQL prefix assignments are not supported");
            }
            if (node.getScopedClause() == null) {
                throw new IllegalArgumentException("Invalid CQL query");
            }
            node.getScopedClause().accept(this);
        }

        @Override
        public void visit(PrefixAssignment node) {
            throw new IllegalArgumentException("CQL prefix assignments are not supported");
        }

        @Override
        public void visit(ScopedClause node) {
            if (node.getScopedClause() == null) {
                node.getSearchClause().accept(this);
                return;
            }

            node.getScopedClause().accept(this);
            String left = fragments.pop();
            node.getSearchClause().accept(this);
            String right = fragments.pop();
            String operator = resolveBooleanOperator(node.getBooleanGroup());
            fragments.push("(%s %s %s)".formatted(left, operator, right));
        }

        private String resolveBooleanOperator(BooleanGroup booleanGroup) {
            if (booleanGroup == null) {
                throw new IllegalArgumentException("Invalid CQL boolean group");
            }
            if (booleanGroup.getModifierList() != null && !booleanGroup.getModifierList().getModifierList().isEmpty()) {
                throw new IllegalArgumentException("CQL boolean modifiers are not supported");
            }
            return switch (booleanGroup.getOperator()) {
                case AND -> "AND";
                case OR -> "OR";
                case NOT -> "AND NOT";
                default -> throw new IllegalArgumentException(
                        "Unsupported CQL boolean operator: " + booleanGroup.getOperator().getToken());
            };
        }

        @Override
        public void visit(BooleanGroup node) {
            throw new IllegalStateException("Boolean groups are handled by scoped clauses");
        }

        @Override
        public void visit(SearchClause node) {
            if (node.getQuery() != null) {
                node.getQuery().accept(this);
                return;
            }
            if (node.getIndex() == null || node.getRelation() == null || node.getTerm() == null) {
                throw new IllegalArgumentException("Unsupported CQL search clause");
            }
            fragments.push(buildComparison(node.getIndex(), node.getRelation(), node.getTerm()));
        }

        private String buildComparison(Index index, Relation relation, Term term) {
            if (relation.getModifierList() != null && !relation.getModifierList().getModifierList().isEmpty()) {
                throw new IllegalArgumentException("CQL relation modifiers are not supported");
            }

            String rawField = index.getName();
            String field = sanitizeFieldName(rawField);
            if (!knownFields.contains(field)) {
                throw new ServiceException(String.format(
                        "Unknown CQL field '%s' for resource type '%s'", rawField, resourceType.getName()));
            }

            String parameterName = "cql_" + cqlParameterIndex++;
            params.addValue(parameterName, coerceValue(field, term.getValue()));

            return switch (relation.getComparitor()) {
                case EQUALS -> field + " = :" + parameterName;
                case NOT_EQUALS -> field + " <> :" + parameterName;
                case GREATER -> field + " > :" + parameterName;
                case GREATER_EQUALS -> field + " >= :" + parameterName;
                case LESS -> field + " < :" + parameterName;
                case LESS_EQUALS -> field + " <= :" + parameterName;
                default -> throw new IllegalArgumentException(
                        "Unsupported CQL operator: " + relation.getComparitor().getToken());
            };
        }

        private Object coerceValue(String fieldName, String rawValue) {
            String normalized = normalizeTermValue(rawValue);
            if (normalized == null) {
                throw new IllegalArgumentException("Missing CQL term value");
            }
            return unwrapListWhenSingle(transformFilterValuesType(resourceType, fieldName, normalized));
        }

        @Override
        public void visit(Relation node) {
            throw new IllegalStateException("Relations are handled by search clauses");
        }

        @Override
        public void visit(Modifier node) {
            throw new IllegalArgumentException("CQL modifiers are not supported");
        }

        @Override
        public void visit(ModifierList node) {
            throw new IllegalArgumentException("CQL modifiers are not supported");
        }

        @Override
        public void visit(Term node) {
            throw new IllegalStateException("Terms are handled by search clauses");
        }

        @Override
        public void visit(Identifier node) {
            throw new IllegalStateException("Identifiers are handled by terms");
        }

        @Override
        public void visit(Index node) {
            throw new IllegalStateException("Indexes are handled by search clauses");
        }

        @Override
        public void visit(SimpleName node) {
            throw new IllegalStateException("Simple names are handled by parent nodes");
        }

        @Override
        public void visit(SortSpec node) {
            throw new IllegalArgumentException("CQL sort clauses are not supported");
        }

        @Override
        public void visit(SingleSpec node) {
            throw new IllegalArgumentException("CQL sort clauses are not supported");
        }
    }
}
