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

import gr.uoa.di.madgik.registry.configuration.SqlSearchProperties;
import gr.uoa.di.madgik.registry.dao.ResourceChunkDao;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Highlight;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunk;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunker;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import gr.uoa.di.madgik.registry.exception.MissingResourceEmbeddingsException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.exception.UnsupportedSearchParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
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
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
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
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

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
    private final float semanticMinScore;

    public DefaultSearchService(@Qualifier("registryDataSource") DataSource dataSource,
                                ResourceTypeService resourceTypeService,
                                SqlFacetService sqlFacetService,
                                EmbeddingService embeddingService,
                                ResourceChunkDao resourceChunkDao,
                                SqlSearchProperties sqlSearchProperties) {
        this.dataSource = dataSource;
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.resourceTypeService = resourceTypeService;
        this.sqlFacetService = sqlFacetService;
        this.resourceRowMapper = new ResourceRowMapper();
        this.embeddingService = embeddingService;
        this.resourceChunkDao = resourceChunkDao;
        this.payloadHighlightContextChars = sqlSearchProperties.getHighlight().getPayloadContextChars();
        this.payloadHighlightMaxFragments = sqlSearchProperties.getHighlight().getPayloadMaxFragments();
        this.semanticSnippetMaxChars = sqlSearchProperties.getHighlight().getSemanticMaxChars();
        this.semanticMinScore = sqlSearchProperties.getSemanticMinScore();
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
        rejectRelevanceSort(filter, "Semantic search");

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
        rejectRelevanceSort(filter, "Hybrid search");

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
        Map<String, List<Highlight>> highlightsById = loadHighlightsByResourceId(paging.getResults(), keyword);

        var resultStream = paging.getResults().stream()
                .map(resource -> {
                    List<Highlight> highlights = highlightsById.getOrDefault(resource.getId(), List.of());
                    return HighlightedResult.of(
                            lexicalHighlightScore(keyword, highlights),
                            resource,
                            highlights
                    );
                });
        if (!hasExplicitOrder(filter)) {
            resultStream = resultStream.sorted(Comparator.comparing(HighlightedResult<Resource>::getScore).reversed());
        }
        List<HighlightedResult<Resource>> results = resultStream.toList();

        return new Paging<>(paging, results);
    }

    @Override
    public Paging<HighlightedResult<Resource>> hybridSearchWithHighlights(FacetFilter filter) throws ServiceException {
        validateQuantity(filter.getQuantity());
        if (!StringUtils.hasText(filter.getKeyword())) {
            return searchWithHighlights(filter);
        }
        rejectRelevanceSort(filter, "Hybrid search");

        List<String> browseBy = resolveBrowseBy(filter);
        List<ResourceType> resourceTypes = getResourceTypes(filter.getResourceType());
        float[] embedding = embeddingService.embed(filter.getKeyword());
        if (!isUsableEmbedding(embedding)) {
            return searchWithHighlights(filter);
        }

        QueryExecutionResult hybrid = executeHybridQuery(filter, browseBy, resourceTypes, embedding);
        List<Resource> resources = hybrid.rows().stream().map(ScoredRow::resource).toList();
        Map<String, List<Highlight>> lexicalHighlightsById = loadHighlightsByResourceId(resources, filter.getKeyword());
        List<HighlightedResult<Resource>> results = hybrid.rows().stream()
                .map(row -> HighlightedResult.of(
                        row.score(),
                        row.resource(),
                        buildHybridHighlights(row, filter.getKeyword(), lexicalHighlightsById.getOrDefault(row.resource().getId(), List.of()))
                ))
                .toList();

        return new Paging<>(hybrid.total(), filter.getFrom(), filter.getFrom() + results.size(), results,
                sqlFacetService.createFacets(browseBy, resourceTypes, hybrid.matchedIds()));
    }

    @Override
    public List<ScoredResult<Resource>> recommend(FacetFilter filter, KeyValue idValue) throws ServiceException {
        validateQuantity(filter.getQuantity());
        ResourceType resourceType = requireSingleResourceType(filter.getResourceType());

        Map<String, Object> sourceRow = getSourceProjection(resourceType, idValue.getField(), idValue.getValue());
        String sourceId = Objects.toString(sourceRow.get("id"), null);
        if (!StringUtils.hasText(sourceId)) {
            throw new ResourceNotFoundException(idValue.getValue(), resourceType.getName());
        }
        if (!hasSourceChunks(sourceId)) {
            throw new MissingResourceEmbeddingsException(sourceId);
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
                           -- (1 + cosine) / 2 maps [-1, 1] → (0, 1], matching Elasticsearch's kNN score formula.
                           MAX((1 + (1 - (rc.embedding <=> sc.embedding))::real) / 2) AS best_similarity
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
                SELECT r.*, scored.score
                FROM candidate_scores scored
                JOIN resource r ON r.id = scored.resource_id
                WHERE scored.score > 0.5
                ORDER BY scored.score DESC, r.modification_date DESC, r.id
                OFFSET :from LIMIT :quantity
                """.formatted(filteredQuery);

        return npJdbcTemplate.query(recommendationQuery, params, (rs, rowNum) -> {
            Resource resource = resourceRowMapper.mapRow(rs, rowNum);
            float score = rs.getFloat("score");
            return ScoredResult.of(score, resource);
        });
    }

    @Override
    public List<ScoredResult<Resource>> recommend(FacetFilter filter, Resource queryResource) throws ServiceException {
        validateQuantity(filter.getQuantity());
        ResourceType resourceType = requireSingleResourceType(filter.getResourceType());

        List<IndexField> indexFields = new ArrayList<>(
                resourceTypeService.getResourceTypeIndexFields(queryResource.getResourceTypeName()));
        List<ResourceEmbeddingChunk> chunks = ResourceEmbeddingChunker.chunk(queryResource, indexFields);
        if (chunks.isEmpty()) {
            throw new ServiceException(
                    "No embeddable fields found for resource type: " + queryResource.getResourceTypeName());
        }
        float[] queryEmbedding = null;
        for (ResourceEmbeddingChunk chunk : chunks) {
            float[] chunkEmbed = embeddingService.embed(chunk.embeddingText());
            if (queryEmbedding == null) {
                queryEmbedding = chunkEmbed.clone();
            } else {
                for (int i = 0; i < queryEmbedding.length; i++) {
                    queryEmbedding[i] += chunkEmbed[i];
                }
            }
        }
        for (int i = 0; i < queryEmbedding.length; i++) {
            queryEmbedding[i] /= chunks.size();
        }
        if (!isUsableEmbedding(queryEmbedding)) {
            throw new ServiceException("Could not compute a usable embedding for the provided resource");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("queryVector", toVectorLiteral(queryEmbedding));
        params.addValue("embeddingModel", embeddingService.modelName());
        params.addValue("from", filter.getFrom());
        params.addValue("quantity", filter.getQuantity());
        params.addValue("keyword_enabled", StringUtils.hasText(filter.getKeyword()));
        params.addValue("keyword_pattern", "%" + Objects.toString(filter.getKeyword(), "").toLowerCase(Locale.ROOT) + "%");

        String filteredQuery = createFilteredResourceScopeQuery(filter, List.of(resourceType), params);
        String recommendationQuery = """
                WITH filtered AS (
                    %s
                ),
                candidate_chunk_scores AS (
                    SELECT rc.resource_id,
                           rc.chunk_idx,
                           -- (1 + cosine) / 2 maps [-1, 1] → (0, 1], matching Elasticsearch's kNN score formula.
                           MAX((1 + (1 - (rc.embedding <=> CAST(:queryVector AS vector)))::real) / 2) AS best_similarity
                    FROM resource_chunk rc
                    JOIN filtered f ON f.id = rc.resource_id
                    WHERE rc.embedding_model = :embeddingModel
                      AND (:keyword_enabled = FALSE OR lower(f.payload) LIKE :keyword_pattern)
                    GROUP BY rc.resource_id, rc.chunk_idx
                ),
                candidate_scores AS (
                    SELECT resource_id,
                           AVG(best_similarity) AS score
                    FROM candidate_chunk_scores
                    GROUP BY resource_id
                )
                SELECT r.*, scored.score
                FROM candidate_scores scored
                JOIN resource r ON r.id = scored.resource_id
                WHERE scored.score > 0.5
                ORDER BY scored.score DESC, r.modification_date DESC, r.id
                OFFSET :from LIMIT :quantity
                """.formatted(filteredQuery);

        return npJdbcTemplate.query(recommendationQuery, params, (rs, rowNum) -> {
            Resource resource = resourceRowMapper.mapRow(rs, rowNum);
            float score = rs.getFloat("score");
            return ScoredResult.of(score, resource);
        });
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
        requireSafeIdentifier(idField);
        requireSafeIdentifier(labelField);
        requireSafeIdentifier(resourceTypeName);

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
        params.addValue("semanticMinScore", semanticMinScore);

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
                    WHERE chunk_rank = 1 AND score >= :semanticMinScore
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
        params.addValue("semanticMinScore", semanticMinScore);

        String filteredQuery = createFilteredResourceScopeQuery(filter, resourceTypes, params);
        String hybridCte = """
                WITH filtered AS (
                    %s
                ),
                lexical_hits AS (
                    SELECT f.id,
                           -- Approximate keyword frequency: count non-overlapping occurrences via
                           -- string-length difference. GREATEST(1, …) ensures the score is never
                           -- zero for a matching document, but note that the numeric value of
                           -- lexical_score is NOT used in the final merged score — only the
                           -- ROW_NUMBER() rank derived from it matters for RRF.
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
                           -- lexical_score is carried here only to drive ORDER BY for ROW_NUMBER().
                           -- The final RRF score in merged_hits uses only lexical_rank, not this value.
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
                    WHERE chunk_rank = 1 AND score >= :semanticMinScore
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
                throw ResourceNotFoundException.unknownResourceType(resourceTypeOrAlias);
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

    private boolean hasSourceChunks(String sourceId) {
        return resourceChunkDao.countByResourceId(sourceId) > 0;
    }

    private ScoredRow mapScoredRow(ResultSet rs, int rowNum) throws SQLException {
        Resource resource = resourceRowMapper.mapRow(rs, rowNum);
        return new ScoredRow(resource, rs.getFloat("score"), rs.getString("semantic_field"), rs.getString("semantic_snippet"));
    }

    private Map<String, List<Highlight>> loadHighlightsByResourceId(List<Resource> resources, String keyword) {
        if (!StringUtils.hasText(keyword) || resources == null || resources.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<Resource>> resourcesByType = resources.stream()
                .filter(resource -> resource != null && StringUtils.hasText(resource.getId()) && StringUtils.hasText(resource.getResourceTypeName()))
                .collect(Collectors.groupingBy(Resource::getResourceTypeName, LinkedHashMap::new, Collectors.toList()));

        Map<String, List<Highlight>> highlightsById = new LinkedHashMap<>();
        for (Map.Entry<String, List<Resource>> entry : resourcesByType.entrySet()) {
            String resourceTypeName = entry.getKey();
            List<String> ids = entry.getValue().stream().map(Resource::getId).toList();
            List<String> fieldNames = getHighlightableFieldNames(resourceTypeName);
            if (fieldNames.isEmpty()) {
                continue;
            }

            String sql = "SELECT id, %s FROM %s_view WHERE id IN (:ids)"
                    .formatted(String.join(", ", fieldNames), resourceTypeName);
            MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
            List<Map<String, Object>> rows = npJdbcTemplate.queryForList(sql, params);
            for (Map<String, Object> row : rows) {
                Object id = row.get("id");
                if (id == null) {
                    continue;
                }
                highlightsById.put(id.toString(), buildHighlights(row, fieldNames, keyword));
            }
        }
        return highlightsById;
    }

    private List<String> getHighlightableFieldNames(String resourceTypeName) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null || resourceType.getIndexFields() == null) {
            return List.of();
        }
        return resourceType.getIndexFields().stream()
                .filter(indexField -> "java.lang.String".equals(indexField.getType()))
                .filter(indexField -> indexField.hasSearchCapability(SearchCapability.KEYWORD)
                        || indexField.hasSearchCapability(SearchCapability.TEXT))
                .map(IndexField::getName)
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    private List<Highlight> buildHighlights(Map<String, Object> row, List<String> fieldNames, String keyword) {
        if (!StringUtils.hasText(keyword) || row == null || fieldNames == null || fieldNames.isEmpty()) {
            return Collections.emptyList();
        }

        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        List<Highlight> highlights = new ArrayList<>();
        for (String fieldName : fieldNames) {
            for (Object value : extractHighlightValues(row.get(fieldName))) {
                if (highlights.size() >= payloadHighlightMaxFragments) {
                    return highlights;
                }
                Highlight highlight = createFieldHighlight(fieldName, Objects.toString(value, null), keyword, lowerKeyword);
                if (highlight != null) {
                    highlights.add(highlight);
                }
            }
        }
        return highlights;
    }

    private float lexicalHighlightScore(String keyword, List<Highlight> highlights) {
        if (!StringUtils.hasText(keyword)) {
            return 0.0f;
        }
        return Math.max(1.0f, highlights == null ? 0.0f : highlights.size());
    }

    private boolean hasExplicitOrder(FacetFilter filter) {
        return filter != null && filter.getOrderBy() != null && !filter.getOrderBy().isEmpty();
    }

    private void rejectRelevanceSort(FacetFilter filter, String searchMode) {
        if (hasExplicitOrder(filter)) {
            throw new UnsupportedSearchParameterException(
                    searchMode + " is relevance-ranked and does not support sort/order parameters.");
        }
    }

    private List<Highlight> buildHybridHighlights(ScoredRow row, String keyword, List<Highlight> lexicalHighlights) {
        if (!StringUtils.hasText(row.semanticSnippet())) {
            return new ArrayList<>(lexicalHighlights);
        }

        String fieldName = StringUtils.hasText(row.semanticField()) ? row.semanticField() : "semantic";
        Highlight semanticHighlight = new Highlight(fieldName, emphasizeKeyword(createSemanticSnippet(row.semanticSnippet()), keyword));
        List<Highlight> highlights = new ArrayList<>(lexicalHighlights.size() + 1);
        boolean merged = false;

        for (Highlight lexicalHighlight : lexicalHighlights) {
            if (!merged && isDuplicateHybridHighlight(lexicalHighlight, semanticHighlight)) {
                highlights.add(preferredHybridHighlight(lexicalHighlight, semanticHighlight));
                merged = true;
            } else {
                highlights.add(lexicalHighlight);
            }
        }

        if (!merged) {
            highlights.add(semanticHighlight);
        }

        return highlights;
    }

    private boolean isDuplicateHybridHighlight(Highlight lexicalHighlight, Highlight semanticHighlight) {
        if (lexicalHighlight == null
                || semanticHighlight == null
                || !Objects.equals(lexicalHighlight.getField(), semanticHighlight.getField())) {
            return false;
        }

        String lexicalValue = normalizeHighlightValue(lexicalHighlight.getValue());
        String semanticValue = normalizeHighlightValue(semanticHighlight.getValue());
        if (!StringUtils.hasText(lexicalValue) || !StringUtils.hasText(semanticValue)) {
            return false;
        }

        return lexicalValue.contains(semanticValue)
                || semanticValue.contains(lexicalValue);
    }

    private Highlight preferredHybridHighlight(Highlight lexicalHighlight, Highlight semanticHighlight) {
        String lexicalValue = normalizeHighlightValue(lexicalHighlight.getValue());
        String semanticValue = normalizeHighlightValue(semanticHighlight.getValue());
        return semanticValue.length() > lexicalValue.length() ? semanticHighlight : lexicalHighlight;
    }

    private String normalizeHighlightValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replace("<em>", "")
                .replace("</em>", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private List<Object> extractHighlightValues(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Array sqlArray) {
            try {
                return extractHighlightValues(sqlArray.getArray());
            } catch (SQLException e) {
                throw new ServiceException("Failed to read SQL array for highlight generation.", e);
            }
        }
        if (value instanceof Object[] arrayValues) {
            return Arrays.stream(arrayValues)
                    .filter(Objects::nonNull)
                    .toList();
        }
        if (value instanceof Collection<?> collectionValues) {
            return collectionValues.stream()
                    .filter(Objects::nonNull)
                    .map(element -> (Object) element)
                    .toList();
        }
        return List.of(value);
    }

    private Highlight createFieldHighlight(String fieldName, String value, String keyword, String lowerKeyword) {
        if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(value)) {
            return null;
        }

        String lowerValue = value.toLowerCase(Locale.ROOT);
        int matchIndex = lowerValue.indexOf(lowerKeyword);
        if (matchIndex < 0) {
            return null;
        }

        int snippetStart = Math.max(0, matchIndex - payloadHighlightContextChars);
        int snippetEnd = Math.min(value.length(), matchIndex + keyword.length() + payloadHighlightContextChars);
        String snippet = value.substring(snippetStart, snippetEnd);

        int snippetMatchStart = matchIndex - snippetStart;
        int snippetMatchEnd = snippetMatchStart + keyword.length();
        String emphasized = snippet.substring(0, snippetMatchStart)
                + "<em>" + snippet.substring(snippetMatchStart, snippetMatchEnd) + "</em>"
                + snippet.substring(snippetMatchEnd);

        return new Highlight(fieldName, emphasized);
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

    private static void requireSafeIdentifier(String name) {
        if (name == null || !SAFE_IDENTIFIER.matcher(name).matches()) {
            throw new ServiceException("Invalid SQL identifier: " + name);
        }
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
