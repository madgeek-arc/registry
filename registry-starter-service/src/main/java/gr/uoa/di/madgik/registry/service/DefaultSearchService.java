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


import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
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
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class DefaultSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchService.class);

    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final DataSource dataSource;
    private final ResourceTypeService resourceTypeService;
    private final SqlFacetService sqlFacetService;
    private final ResourceRowMapper resourceRowMapper;

    public DefaultSearchService(@Qualifier("registryDataSource") DataSource dataSource,
                                ResourceTypeService resourceTypeService,
                                SqlFacetService sqlFacetService) {
        this.dataSource = dataSource;
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.resourceTypeService = resourceTypeService;
        this.sqlFacetService = sqlFacetService;
        this.resourceRowMapper = new ResourceRowMapper();
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

        Integer total = npJdbcTemplate.queryForObject(sqlQuery.countQuery(), sqlQuery.params(),
                new SingleColumnRowMapper<>(Integer.class));
        List<Resource> resources = npJdbcTemplate.query(sqlQuery.resultQuery(), sqlQuery.params(), resourceRowMapper);

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

        Integer total = npJdbcTemplate.queryForObject(sqlQuery.countQuery(), sqlQuery.params(),
                new SingleColumnRowMapper<>(Integer.class));
        List<Resource> resources = npJdbcTemplate.query(sqlQuery.resultQuery(), sqlQuery.params(), resourceRowMapper);
        List<String> matchedIds = browseBy.isEmpty()
                ? List.of()
                : npJdbcTemplate.queryForList(
                        "SELECT ar.id FROM (%s) ar WHERE ar.payload LIKE :keyword".formatted(sqlQuery.nestedQuery()),
                        sqlQuery.params(),
                        String.class
                );
        return new Paging<>(total, filter.getFrom(), filter.getFrom() + filter.getQuantity(), resources,
                sqlFacetService.createFacets(browseBy, resourceTypes, matchedIds));
    }

    @Override
    public Paging<Resource> semanticSearch(FacetFilter filter) throws ServiceException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support semantic search.");
    }

    @Override
    public Paging<Resource> hybridSearch(FacetFilter filter) throws ServiceException {
        return search(filter);
    }

    @Override
    public Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) throws ServiceException {
        Paging<Resource> paging = search(filter);
        String keyword = filter.getKeyword();

        List<HighlightedResult<Resource>> results = paging.getResults().stream()
                .map(resource -> HighlightedResult.of(
                        StringUtils.hasText(keyword) ? 1.0f : 0.0f,
                        resource,
                        buildPayloadHighlights(resource, keyword)
                ))
                .toList();

        return new Paging<>(paging, results);
    }

    @Override
    public List<Resource> recommend(FacetFilter filter, KeyValue idValue) throws ServiceException {
        validateQuantity(filter.getQuantity());
        ResourceType resourceType = requireSingleResourceType(filter.getResourceType());
        String sourceField = normalizeLookupField(idValue.getField());

        Map<String, Object> sourceRow = getSourceProjection(resourceType, sourceField, idValue.getValue());
        String sourceId = Objects.toString(sourceRow.get("id"), null);
        if (!StringUtils.hasText(sourceId)) {
            throw new ResourceNotFoundException(idValue.getValue(), resourceType.getName());
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("keyword", StringUtils.hasText(filter.getKeyword()) ? "%" + filter.getKeyword() + "%" : "%");
        params.addValue("source_id", sourceId);
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());

        SearchSqlQueryBuilder.SearchSqlQuery sqlQuery = SearchSqlQueryBuilder.builder(dataSource)
                .withFilter(filter)
                .withResourceTypes(List.of(resourceType))
                .withParameters(params)
                .buildSearchQuery();

        String scoreExpression = buildRecommendationScoreExpression(resourceType, sourceRow, params);
        if (!StringUtils.hasText(scoreExpression)) {
            return Collections.emptyList();
        }

        String candidateQuery = "SELECT * FROM (%s) ar WHERE ar.payload LIKE :keyword".formatted(sqlQuery.nestedQuery());
        String scoredQuery = """
                SELECT ar.id, %s AS score
                FROM (%s) ar
                INNER JOIN %s_view v ON ar.id = v.id
                WHERE ar.id <> :source_id
                """.formatted(scoreExpression, candidateQuery, resourceType.getName());
        String recommendationQuery = """
                SELECT r.*
                FROM (%s) scored
                INNER JOIN resource r ON r.id = scored.id
                WHERE scored.score > 0
                ORDER BY scored.score DESC, r.modification_date DESC
                OFFSET :from LIMIT :quantity
                """.formatted(scoredQuery);

        return npJdbcTemplate.query(recommendationQuery, sqlQuery.params(), resourceRowMapper);
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
            result = npJdbcTemplate.queryForObject(sqlQuery.resultQuery(), sqlQuery.params(), resourceRowMapper);
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

    private List<Highlight> buildPayloadHighlights(Resource resource, String keyword) {
        if (!StringUtils.hasText(keyword) || !StringUtils.hasText(resource.getPayload())) {
            return Collections.emptyList();
        }

        String payload = resource.getPayload();
        String lowerPayload = payload.toLowerCase(Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        List<Highlight> highlights = new ArrayList<>();
        int fromIndex = 0;

        while (highlights.size() < 5) {
            int matchIndex = lowerPayload.indexOf(lowerKeyword, fromIndex);
            if (matchIndex < 0) {
                break;
            }

            int snippetStart = Math.max(0, matchIndex - 80);
            int snippetEnd = Math.min(payload.length(), matchIndex + keyword.length() + 80);
            String snippet = payload.substring(snippetStart, snippetEnd);

            int snippetMatchStart = matchIndex - snippetStart;
            int snippetMatchEnd = snippetMatchStart + keyword.length();
            String emphasized = snippet.substring(0, snippetMatchStart)
                    + "<em>" + snippet.substring(snippetMatchStart, snippetMatchEnd) + "</em>"
                    + snippet.substring(snippetMatchEnd);

            highlights.add(new Highlight("payload", emphasized));
            fromIndex = matchIndex + keyword.length();
        }

        return highlights;
    }

    private ResourceType requireSingleResourceType(String resourceTypeOrAlias) {
        List<ResourceType> resourceTypes = getResourceTypes(resourceTypeOrAlias);
        if (resourceTypes.size() != 1) {
            throw new ServiceException("Recommendations require a concrete resource type, not an alias group.");
        }
        return resourceTypes.getFirst();
    }

    private String normalizeLookupField(String field) {
        if ("resource_internal_id".equals(field)) {
            return "id";
        }
        return field;
    }

    private Map<String, Object> getSourceProjection(ResourceType resourceType, String field, String value) {
        Set<String> knownFields = resourceType.getIndexFields().stream()
                .map(IndexField::getName)
                .collect(Collectors.toSet());

        if (!"id".equals(field) && !knownFields.contains(field)) {
            throw new ServiceException(
                    String.format("Unknown recommendation field '%s' for resource type '%s'", field, resourceType.getName()));
        }

        String sql = "SELECT * FROM %s_view WHERE %s = :value LIMIT 1".formatted(resourceType.getName(), field);
        try {
            return npJdbcTemplate.queryForMap(sql, new MapSqlParameterSource("value", value));
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(value, resourceType.getName());
        }
    }

    private String buildRecommendationScoreExpression(ResourceType resourceType,
                                                      Map<String, Object> sourceRow,
                                                      MapSqlParameterSource params) {
        List<String> scoreTerms = new ArrayList<>();

        for (IndexField field : resourceType.getIndexFields()) {
            if (field.isPrimaryKey() || field.isMultivalued() || "embedding".equals(field.getType())) {
                continue;
            }

            Object value = sourceRow.get(field.getName());
            if (value == null) {
                continue;
            }

            String paramName = "score_" + field.getName();
            params.addValue(paramName, normalizeScoreValue(value));
            scoreTerms.add("CASE WHEN v.%s = :%s THEN 1 ELSE 0 END".formatted(field.getName(), paramName));
        }

        return String.join(" + ", scoreTerms);
    }

    private Object normalizeScoreValue(Object value) {
        if (value instanceof Date date) {
            return new Timestamp(date.getTime());
        }
        return value;
    }
}
