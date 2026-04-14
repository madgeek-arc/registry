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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
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
import java.util.stream.Collectors;


@Service
public class DefaultSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchService.class);

    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final DataSource dataSource;
    private final ObjectMapper mapper;
    private final ResourceTypeService resourceTypeService;

    public DefaultSearchService(@Qualifier("registryDataSource") DataSource dataSource,
                                ResourceTypeService resourceTypeService) {
        this.dataSource = dataSource;
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
        if (StringUtils.hasText(sortByField)) {
            filter.setOrderBy(FacetFilter.createOrderBy(List.of(sortByField), List.of(sortOrder)));
        }

        SearchSqlQueryBuilder.SearchSqlQuery sqlQuery = SearchSqlQueryBuilder.builder(dataSource)
                .withFilter(filter)
                .withResourceTypes(getResourceTypes(filter.getResourceType()))
                .withParameters(params)
                .withCqlQuery(query)
                .buildCqlQuery();

        Integer total = 0;
        List<Resource> resources;

        total = npJdbcTemplate.queryForObject(sqlQuery.countQuery(), sqlQuery.params(), new SingleColumnRowMapper<>(Integer.class));
        List<Map<String, Object>> results = npJdbcTemplate.queryForList(sqlQuery.resultQuery(), sqlQuery.params());
        resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).toList();

        return new Paging<>(total, from, from + quantity, resources, new ArrayList<>());
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        return cqlQuery(query, resourceType, 100, 0, "", "ASC");
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) {
        validateQuantity(filter.getQuantity());
        List<String> browseBy = resolveBrowseBy(filter);
        List<ResourceType> resourceTypes = getResourceTypes(filter.getResourceType());
        String keyword = StringUtils.hasText(filter.getKeyword())
                ? "%" + filter.getKeyword() + "%"
                : "%";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("keyword", keyword);
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());

        SearchSqlQueryBuilder.SearchSqlQuery sqlQuery = SearchSqlQueryBuilder.builder(dataSource)
                .withFilter(filter)
                .withResourceTypes(resourceTypes)
                .withParameters(params)
                .buildSearchQuery();

        Integer total = npJdbcTemplate.queryForObject(sqlQuery.countQuery(), sqlQuery.params(), new SingleColumnRowMapper<>(Integer.class));
        List<Map<String, Object>> results = npJdbcTemplate.queryForList(sqlQuery.resultQuery(), sqlQuery.params());

        List<Resource> resources = results.stream().map(r -> mapper.convertValue(r, Resource.class)).toList();
        return new Paging<>(total, filter.getFrom(), filter.getFrom() + filter.getQuantity(), resources,
                createFacets(browseBy, resourceTypes, sqlQuery));
    }

    @Override
    public Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) throws ServiceException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support highlighted search.");
    }

    @Override
    public List<Resource> recommend(FacetFilter filter, KeyValue idValue) throws ServiceException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support recommendations.");
    }

    private List<Facet> createFacets(List<String> browseBy, List<ResourceType> resourceTypes,
                                     SearchSqlQueryBuilder.SearchSqlQuery sqlQuery) {
        if (browseBy == null || browseBy.isEmpty()) {
            return new ArrayList<>();
        }
        List<Facet> facets = new ArrayList<>();
        for (String browse : browseBy) {
            IndexField indexField = findFacetField(resourceTypes, browse);
            if (indexField == null) {
                continue;
            }
            Facet facet = new Facet();
            facet.setField(browse);
            facet.setLabel(indexField.getLabel());
            facet.setValues(loadFacetValues(indexField, sqlQuery));
            facets.add(facet);
        }
        return facets;
    }

    private IndexField findFacetField(List<ResourceType> resourceTypes, String fieldName) {
        for (ResourceType resourceType : resourceTypes) {
            for (IndexField indexField : resourceTypeService.getResourceTypeIndexFields(resourceType.getName())) {
                if (fieldName.equals(indexField.getName())) {
                    return indexField;
                }
            }
        }
        return null;
    }

    private List<Value> loadFacetValues(IndexField field, SearchSqlQueryBuilder.SearchSqlQuery sqlQuery) {
        String matchedIdsQuery = "SELECT ar.id FROM (%s) ar WHERE ar.payload LIKE :keyword".formatted(sqlQuery.nestedQuery());
        String valueExpression = field.isMultivalued() ? "facet_value" : "view_row.%s".formatted(field.getName());
        String joinExpression = field.isMultivalued()
                ? "CROSS JOIN LATERAL unnest(view_row.%s) AS facet_value ".formatted(field.getName())
                : "";
        String countExpression = field.isMultivalued() ? "COUNT(DISTINCT matched.id)" : "COUNT(*)";
        String sql = """
                SELECT CAST(%s AS text) AS value, %s AS count
                FROM (%s) matched
                INNER JOIN %s_view view_row ON view_row.id = matched.id
                %s
                WHERE %s IS NOT NULL
                GROUP BY %s
                ORDER BY count DESC, value ASC
                """.formatted(
                valueExpression,
                countExpression,
                matchedIdsQuery,
                field.getResourceType().getName(),
                joinExpression,
                valueExpression,
                valueExpression
        );

        List<Value> values = npJdbcTemplate.query(sql, sqlQuery.params(), (rs, rowNum) -> {
            Value value = new Value();
            value.setValue(rs.getString("value"));
            value.setCount(rs.getLong("count"));
            return value;
        });
        Collections.sort(values);
        Collections.reverse(values);
        return values;
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
    @Retryable(retryFor = ServiceException.class, backoff = @Backoff(value = 200))
    public Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("@Retryable 'searchId(resourceType={}, ids={{}})'",
                    resourceType,
                    String.join(",", Arrays
                            .stream(fields)
                            .map(keyValue -> keyValue.getField() + "=" + keyValue.getValue())
                            .collect(Collectors.toSet()))
            );
        }

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType);
        filter.setFrom(0);
        filter.setQuantity(1);
        for (KeyValue keyValue : fields) {
            filter.addFilter(keyValue.getField(), keyValue.getValue());
        }

        MapSqlParameterSource params = new MapSqlParameterSource();

        SearchSqlQueryBuilder.SearchSqlQuery sqlQuery = SearchSqlQueryBuilder.builder(dataSource)
                .withFilter(filter)
                .withResourceTypes(getResourceTypes(filter.getResourceType()))
                .withParameters(params)
                .buildSingleResultQuery();

        Resource result = null;
        try {
            result = npJdbcTemplate.queryForObject(sqlQuery.resultQuery(), sqlQuery.params(), new ResourceRowMapper());
        } catch (EmptyResultDataAccessException _) {
            return null; // when no result is found
        } catch (Exception e) {
            throw new ServiceException("Failed to search fields for resource type: " + resourceType, e);
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

        ResourceType resolvedResourceType = resourceTypeService.getResourceType(resourceType);
        if (resolvedResourceType == null) {
            throw new ServiceException(String.format("Unknown resource type '%s'", resourceType));
        }
        String resourceTypeName = resolvedResourceType.getName();

        Set<String> knownFields = resourceTypeService.getResourceTypeIndexFields(resourceTypeName)
                .stream()
                .map(IndexField::getName)
                .collect(Collectors.toSet());

        if (!knownFields.contains(idField)) {
            throw new ServiceException(
                    String.format("Unknown idField '%s' for resource type '%s'", idField, resourceTypeName));
        }
        if (!knownFields.contains(labelField)) {
            throw new ServiceException(
                    String.format("Unknown labelField '%s' for resource type '%s'", labelField, resourceTypeName));
        }

        // Field names are validated above against the registry's own metadata —
        // interpolation here is intentional and safe.
        String sql = String.format(
                "SELECT %s, %s FROM %s_view WHERE %s IN (:ids)",
                idField, labelField, resourceTypeName, idField);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(sql, params);
        Map<String, String> result = HashMap.newHashMap(rows.size());
        for (Map<String, Object> row : rows) {
            Object id = row.get(idField);
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

    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    private static class ResourcePropertyName extends PropertyNamingStrategies.NamingBase {

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
