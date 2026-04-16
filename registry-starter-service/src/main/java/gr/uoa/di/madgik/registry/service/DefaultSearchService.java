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

import gr.uoa.di.madgik.registry.configuration.SqlSearchHighlightProperties;
import gr.uoa.di.madgik.registry.dao.ResourceChunkDao;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Highlight;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class DefaultSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchService.class);
    private static final int HYBRID_RRF_K = 60;

    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final DataSource dataSource;
    private final ResourceTypeService resourceTypeService;
    private final SqlFacetService sqlFacetService;
    private final ResourceRowMapper resourceRowMapper;
    private final EmbeddingService embeddingService;
    private final ResourceChunkDao resourceChunkDao;
    private final int payloadHighlightContextChars;
    private final int payloadHighlightMaxFragments;
    private final int semanticSnippetMaxChars;

    public DefaultSearchService(@Qualifier("registryDataSource") DataSource dataSource,
                                ResourceTypeService resourceTypeService,
                                SqlFacetService sqlFacetService,
                                EmbeddingService embeddingService,
                                ResourceChunkDao resourceChunkDao,
                                SqlSearchHighlightProperties highlightProperties) {
        this.dataSource = dataSource;
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.resourceTypeService = resourceTypeService;
        this.sqlFacetService = sqlFacetService;
        this.resourceRowMapper = new ResourceRowMapper();
        this.embeddingService = embeddingService;
        this.resourceChunkDao = resourceChunkDao;
        this.payloadHighlightContextChars = highlightProperties.getPayloadContextChars();
        this.payloadHighlightMaxFragments = highlightProperties.getPayloadMaxFragments();
        this.semanticSnippetMaxChars = highlightProperties.getSemanticMaxChars();
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

        return new Paging<>(total == null ? 0 : total, from, from + resources.size(), resources, new ArrayList<>());
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
        return new Paging<>(total == null ? 0 : total, filter.getFrom(), filter.getFrom() + resources.size(), resources,
                sqlFacetService.createFacets(browseBy, resourceTypes, matchedIds));
    }

    @Override
    public Paging<Resource> semanticSearch(FacetFilter filter) throws ServiceException {
        validateQuantity(filter.getQuantity());
        if (!StringUtils.hasText(filter.getKeyword())) {
            throw new ServiceException("Semantic search requires a non-empty keyword.");
        }

        List<String> browseBy = resolveBrowseBy(filter);
        List<ResourceType> resourceTypes = getResourceTypes(filter.getResourceType());
        float[] embedding = embeddingService.embed(filter.getKeyword());
        if (!isUsableEmbedding(embedding)) {
            return emptyPaging(filter, browseBy, resourceTypes);
        }

        QueryExecutionResult semantic = executeSemanticQuery(filter, browseBy, resourceTypes, embedding);
        List<Resource> resources = semantic.rows().stream().map(ScoredRow::resource).toList();
        return new Paging<>(semantic.total(), filter.getFrom(), filter.getFrom() + resources.size(), resources,
                sqlFacetService.createFacets(browseBy, resourceTypes, semantic.matchedIds()));
    }

    @Override
    public Paging<Resource> hybridSearch(FacetFilter filter) throws ServiceException {
        validateQuantity(filter.getQuantity());
        if (!StringUtils.hasText(filter.getKeyword())) {
            return search(filter);
        }

        List<String> browseBy = resolveBrowseBy(filter);
        List<ResourceType> resourceTypes = getResourceTypes(filter.getResourceType());
        float[] embedding = embeddingService.embed(filter.getKeyword());
        if (!isUsableEmbedding(embedding)) {
            return search(filter);
        }

        QueryExecutionResult hybrid = executeHybridQuery(filter, browseBy, resourceTypes, embedding);
        List<Resource> resources = hybrid.rows().stream().map(ScoredRow::resource).toList();
        return new Paging<>(hybrid.total(), filter.getFrom(), filter.getFrom() + resources.size(), resources,
                sqlFacetService.createFacets(browseBy, resourceTypes, hybrid.matchedIds()));
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
    public Paging<HighlightedResult<Resource>> hybridSearchWithHighlights(FacetFilter filter) throws ServiceException {
        validateQuantity(filter.getQuantity());
        if (!StringUtils.hasText(filter.getKeyword())) {
            return searchWithHighlights(filter);
        }

        List<String> browseBy = resolveBrowseBy(filter);
        List<ResourceType> resourceTypes = getResourceTypes(filter.getResourceType());
        float[] embedding = embeddingService.embed(filter.getKeyword());
        if (!isUsableEmbedding(embedding)) {
            return searchWithHighlights(filter);
        }

        QueryExecutionResult hybrid = executeHybridQuery(filter, browseBy, resourceTypes, embedding);
        List<HighlightedResult<Resource>> results = hybrid.rows().stream()
                .map(row -> HighlightedResult.of(row.score(), row.resource(), buildHybridHighlights(row, filter.getKeyword())))
                .toList();

        return new Paging<>(hybrid.total(), filter.getFrom(), filter.getFrom() + results.size(), results,
                sqlFacetService.createFacets(browseBy, resourceTypes, hybrid.matchedIds()));
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
        if (countSourceChunks(sourceId) == 0) {
            throw new ResourceNotFoundException(
                    "There are no recommendations available for this resource",
                    new UnsupportedOperationException("Embedding chunks are missing for the requested resource")
            );
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("source_id", sourceId);
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());
        params.addValue("keyword_enabled", StringUtils.hasText(filter.getKeyword()));
        params.addValue("keyword_pattern", "%" + Objects.toString(filter.getKeyword(), "").toLowerCase(Locale.ROOT) + "%");

        String filteredQuery = createFilteredResourceScopeQuery(filter, List.of(resourceType), params);
        String recommendationQuery = """
                WITH filtered AS (
                    %s
                ),
                source_chunks AS (
                    SELECT embedding, embedding_model
                    FROM resource_chunk
                    WHERE resource_id = :source_id
                ),
                candidate_chunk_scores AS (
                    SELECT rc.resource_id,
                           rc.chunk_idx,
                           MAX((1 - (rc.embedding <=> sc.embedding))::real) AS best_similarity
                    FROM resource_chunk rc
                    JOIN filtered f ON f.id = rc.resource_id
                    JOIN source_chunks sc ON sc.embedding_model = rc.embedding_model
                    WHERE rc.resource_id <> :source_id
                      AND (:keyword_enabled = FALSE OR lower(f.payload) LIKE :keyword_pattern)
                    GROUP BY rc.resource_id, rc.chunk_idx
                ),
                candidate_scores AS (
                    SELECT resource_id,
                           AVG(best_similarity) AS score
                    FROM candidate_chunk_scores
                    GROUP BY resource_id
                )
                SELECT r.*
                FROM candidate_scores scored
                JOIN resource r ON r.id = scored.resource_id
                WHERE scored.score > 0
                ORDER BY scored.score DESC, r.modification_date DESC, r.id
                OFFSET :from LIMIT :quantity
                """.formatted(filteredQuery);

        return npJdbcTemplate.query(recommendationQuery, params, resourceRowMapper);
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
                    String.join(",", Arrays.stream(fields)
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

        try {
            return npJdbcTemplate.queryForObject(sqlQuery.resultQuery(), sqlQuery.params(), resourceRowMapper);
        } catch (EmptyResultDataAccessException _) {
            return null;
        } catch (Exception e) {
            throw new ServiceException("Failed to search fields for resource type: " + resourceType, e);
        }
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

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

        String sql = String.format(
                "SELECT %s, %s FROM %s_view WHERE %s IN (:ids)",
                idField, labelField, resourceTypeName, idField);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(sql, params);
        Map<String, String> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get(idField);
            Object label = row.get(labelField);
            if (id != null && label != null) {
                result.put(id.toString(), label.toString());
            }
        }
        return result;
    }

    private QueryExecutionResult executeSemanticQuery(FacetFilter filter,
                                                      List<String> browseBy,
                                                      List<ResourceType> resourceTypes,
                                                      float[] embedding) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());
        params.addValue("queryVector", toVectorLiteral(embedding));
        params.addValue("embeddingModel", embeddingService.modelName());

        String filteredQuery = createFilteredResourceScopeQuery(filter, resourceTypes, params);
        String semanticCte = """
                WITH filtered AS (
                    %s
                ),
                semantic_hits AS (
                    SELECT f.id,
                           rc.field_name,
                           rc.content,
                           (1 - (rc.embedding <=> CAST(:queryVector AS vector)))::real AS score,
                           ROW_NUMBER() OVER (
                               PARTITION BY f.id
                               ORDER BY rc.embedding <=> CAST(:queryVector AS vector), rc.chunk_idx
                           ) AS chunk_rank
                    FROM filtered f
                    JOIN resource_chunk rc ON rc.resource_id = f.id
                    WHERE rc.embedding_model = :embeddingModel
                ),
                best_semantic_hits AS (
                    SELECT id, field_name, content, score
                    FROM semantic_hits
                    WHERE chunk_rank = 1 AND score > 0
                )
                """.formatted(filteredQuery);

        Integer total = npJdbcTemplate.queryForObject(semanticCte + " SELECT COUNT(*) FROM best_semantic_hits", params, Integer.class);
        if (total == null || total == 0) {
            return new QueryExecutionResult(0, List.of(), List.of());
        }

        List<String> matchedIds = browseBy.isEmpty()
                ? List.of()
                : npJdbcTemplate.queryForList(semanticCte + " SELECT id FROM best_semantic_hits", params, String.class);

        String resultQuery = semanticCte + """
                SELECT f.*,
                       b.score,
                       b.field_name AS semantic_field,
                       b.content AS semantic_snippet
                FROM filtered f
                JOIN best_semantic_hits b ON b.id = f.id
                ORDER BY b.score DESC, f.modification_date DESC, f.id
                OFFSET :from LIMIT :quantity
                """;
        List<ScoredRow> rows = npJdbcTemplate.query(resultQuery, params, this::mapScoredRow);
        return new QueryExecutionResult(total, matchedIds, rows);
    }

    private QueryExecutionResult executeHybridQuery(FacetFilter filter,
                                                    List<String> browseBy,
                                                    List<ResourceType> resourceTypes,
                                                    float[] embedding) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());
        params.addValue("queryVector", toVectorLiteral(embedding));
        params.addValue("embeddingModel", embeddingService.modelName());
        params.addValue("keywordText", filter.getKeyword().toLowerCase(Locale.ROOT));
        params.addValue("keywordPattern", "%" + filter.getKeyword().toLowerCase(Locale.ROOT) + "%");
        params.addValue("rrfK", HYBRID_RRF_K);

        String filteredQuery = createFilteredResourceScopeQuery(filter, resourceTypes, params);
        String hybridCte = """
                WITH filtered AS (
                    %s
                ),
                lexical_hits AS (
                    SELECT f.id,
                           GREATEST(
                               1,
                               (length(lower(f.payload)) - length(replace(lower(f.payload), :keywordText, '')))
                               / NULLIF(length(:keywordText), 0)
                           )::real AS lexical_score
                    FROM filtered f
                    WHERE lower(f.payload) LIKE :keywordPattern
                ),
                lexical_ranked AS (
                    SELECT l.id,
                           l.lexical_score,
                           ROW_NUMBER() OVER (ORDER BY l.lexical_score DESC, f.modification_date DESC, l.id) AS lexical_rank
                    FROM lexical_hits l
                    JOIN filtered f ON f.id = l.id
                ),
                semantic_hits AS (
                    SELECT f.id,
                           rc.field_name,
                           rc.content,
                           (1 - (rc.embedding <=> CAST(:queryVector AS vector)))::real AS score,
                           ROW_NUMBER() OVER (
                               PARTITION BY f.id
                               ORDER BY rc.embedding <=> CAST(:queryVector AS vector), rc.chunk_idx
                           ) AS chunk_rank
                    FROM filtered f
                    JOIN resource_chunk rc ON rc.resource_id = f.id
                    WHERE rc.embedding_model = :embeddingModel
                ),
                best_semantic_hits AS (
                    SELECT id, field_name, content, score
                    FROM semantic_hits
                    WHERE chunk_rank = 1 AND score > 0
                ),
                semantic_ranked AS (
                    SELECT s.id,
                           s.field_name AS semantic_field,
                           s.content AS semantic_snippet,
                           s.score AS semantic_score,
                            ROW_NUMBER() OVER (ORDER BY s.score DESC, f.modification_date DESC, s.id) AS semantic_rank
                    FROM best_semantic_hits s
                    JOIN filtered f ON f.id = s.id
                ),
                merged_hits AS (
                    SELECT COALESCE(l.id, s.id) AS id,
                           COALESCE(1.0 / (:rrfK + l.lexical_rank), 0.0)
                             + COALESCE(1.0 / (:rrfK + s.semantic_rank), 0.0) AS score,
                           s.semantic_field,
                           s.semantic_snippet
                    FROM lexical_ranked l
                    FULL OUTER JOIN semantic_ranked s ON s.id = l.id
                )
                """.formatted(filteredQuery);

        Integer total = npJdbcTemplate.queryForObject(hybridCte + " SELECT COUNT(*) FROM merged_hits", params, Integer.class);
        if (total == null || total == 0) {
            return new QueryExecutionResult(0, List.of(), List.of());
        }

        List<String> matchedIds = browseBy.isEmpty()
                ? List.of()
                : npJdbcTemplate.queryForList(hybridCte + " SELECT id FROM merged_hits", params, String.class);

        String resultQuery = hybridCte + """
                SELECT f.*,
                       m.score,
                       m.semantic_field,
                       m.semantic_snippet
                FROM filtered f
                JOIN merged_hits m ON m.id = f.id
                ORDER BY m.score DESC, f.modification_date DESC, f.id
                OFFSET :from LIMIT :quantity
                """;
        List<ScoredRow> rows = npJdbcTemplate.query(resultQuery, params, this::mapScoredRow);
        return new QueryExecutionResult(total, matchedIds, rows);
    }

    private FacetFilter createStructuredScopeFilter(FacetFilter filter) {
        FacetFilter scope = new FacetFilter();
        scope.setResourceType(filter.getResourceType());
        scope.setFilter(new LinkedHashMap<>(filter.getFilter()));
        scope.setRangeFilters(new LinkedHashMap<>(filter.getRangeFilters()));
        return scope;
    }

    private String createFilteredResourceScopeQuery(FacetFilter filter,
                                                    List<ResourceType> resourceTypes,
                                                    MapSqlParameterSource params) {
        SearchSqlQueryBuilder.SearchSqlQuery sqlQuery = SearchSqlQueryBuilder.builder(dataSource)
                .withFilter(createStructuredScopeFilter(filter))
                .withResourceTypes(resourceTypes)
                .withParameters(params)
                .buildSingleResultQuery();
        return sqlQuery.nestedQuery();
    }

    private List<String> getResourceTypeNames(String resourceTypeOrAlias) {
        return getResourceTypes(resourceTypeOrAlias).stream().map(ResourceType::getName).toList();
    }

    private List<ResourceType> getResourceTypes(String resourceTypeOrAlias) {
        List<ResourceType> resourceTypes = new ArrayList<>();

        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeOrAlias);
        if (resourceType == null) {
            resourceTypes = resourceTypeService.getAllResourceTypeByAlias(resourceTypeOrAlias);
            if (resourceTypes.isEmpty()) {
                throw new ServiceException("No resource types found for alias: " + resourceTypeOrAlias);
            }
        } else {
            resourceTypes.add(resourceType);
        }
        return resourceTypes;
    }

    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    private Paging<Resource> emptyPaging(FacetFilter filter,
                                         List<String> browseBy,
                                         List<ResourceType> resourceTypes) {
        return new Paging<>(0, filter.getFrom(), filter.getFrom(), List.of(),
                sqlFacetService.createFacets(browseBy, resourceTypes, List.of()));
    }

    private boolean isUsableEmbedding(float[] embedding) {
        if (embedding == null || embedding.length != EmbeddingService.VECTOR_SIZE) {
            return false;
        }
        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private String toVectorLiteral(float[] embedding) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : embedding) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }

    private int countSourceChunks(String sourceId) {
        return Math.toIntExact(resourceChunkDao.countByResourceId(sourceId));
    }

    private ScoredRow mapScoredRow(ResultSet rs, int rowNum) throws SQLException {
        Resource resource = resourceRowMapper.mapRow(rs, rowNum);
        return new ScoredRow(resource, rs.getFloat("score"), rs.getString("semantic_field"), rs.getString("semantic_snippet"));
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

        while (highlights.size() < payloadHighlightMaxFragments) {
            int matchIndex = lowerPayload.indexOf(lowerKeyword, fromIndex);
            if (matchIndex < 0) {
                break;
            }

            int snippetStart = Math.max(0, matchIndex - payloadHighlightContextChars);
            int snippetEnd = Math.min(payload.length(), matchIndex + keyword.length() + payloadHighlightContextChars);
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

    private List<Highlight> buildHybridHighlights(ScoredRow row, String keyword) {
        List<Highlight> highlights = new ArrayList<>(buildPayloadHighlights(row.resource(), keyword));
        if (StringUtils.hasText(row.semanticSnippet())) {
            String fieldName = StringUtils.hasText(row.semanticField()) ? row.semanticField() : "semantic";
            highlights.add(new Highlight(fieldName,
                    emphasizeKeyword(createSemanticSnippet(row.semanticSnippet()), keyword)));
        }
        return highlights;
    }

    private String createSemanticSnippet(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= semanticSnippetMaxChars) {
            return normalized;
        }
        return normalized.substring(0, semanticSnippetMaxChars).trim() + "...";
    }

    private String emphasizeKeyword(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return text;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        int matchIndex = lowerText.indexOf(lowerKeyword);
        if (matchIndex < 0) {
            return text;
        }

        int matchEnd = matchIndex + keyword.length();
        return text.substring(0, matchIndex)
                + "<em>" + text.substring(matchIndex, matchEnd) + "</em>"
                + text.substring(matchEnd);
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

    private static final class QueryExecutionResult {
        private final int total;
        private final List<String> matchedIds;
        private final List<ScoredRow> rows;

        private QueryExecutionResult(int total, List<String> matchedIds, List<ScoredRow> rows) {
            this.total = total;
            this.matchedIds = matchedIds;
            this.rows = rows;
        }

        private int total() {
            return total;
        }

        private List<String> matchedIds() {
            return matchedIds;
        }

        private List<ScoredRow> rows() {
            return rows;
        }
    }

    private static final class ScoredRow {
        private final Resource resource;
        private final float score;
        private final String semanticField;
        private final String semanticSnippet;

        private ScoredRow(Resource resource, float score, String semanticField, String semanticSnippet) {
            this.resource = resource;
            this.score = score;
            this.semanticField = semanticField;
            this.semanticSnippet = semanticSnippet;
        }

        private Resource resource() {
            return resource;
        }

        private float score() {
            return score;
        }

        private String semanticField() {
            return semanticField;
        }

        private String semanticSnippet() {
            return semanticSnippet;
        }
    }
}
