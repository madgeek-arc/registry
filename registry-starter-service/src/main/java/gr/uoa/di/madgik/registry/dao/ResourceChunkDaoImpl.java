/*
 * Copyright 2026-2026 OpenAIRE AMKE
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

package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceChunk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.StringJoiner;

@Repository("resourceChunkDao")
@Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
public class ResourceChunkDaoImpl implements ResourceChunkDao {

    private static final String DELETE_SQL = "DELETE FROM resource_chunk WHERE resource_id = :resourceId";
    private static final String INSERT_SQL = """
            INSERT INTO resource_chunk (
                resource_id, chunk_idx, field_name, value_ordinal, field_chunk_idx, content, embedding, embedding_model
            )
            VALUES (
                :resourceId, :chunkIdx, :fieldName, :valueOrdinal, :fieldChunkIdx, :content, CAST(:embedding AS vector), :embeddingModel
            )
            """;
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM resource_chunk WHERE resource_id = :resourceId";
    private static final String FIND_BY_RESOURCE_ID_SQL = """
            SELECT id, resource_id, chunk_idx, field_name, value_ordinal, field_chunk_idx, content, embedding_model, created_at
            FROM resource_chunk
            WHERE resource_id = :resourceId
            ORDER BY chunk_idx
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ResourceChunk> rowMapper = (rs, _) -> {
        ResourceChunk chunk = new ResourceChunk();
        chunk.setId(rs.getLong("id"));
        chunk.setResourceId(rs.getString("resource_id"));
        chunk.setChunkIdx(rs.getInt("chunk_idx"));
        chunk.setFieldName(rs.getString("field_name"));
        chunk.setValueOrdinal(rs.getInt("value_ordinal"));
        chunk.setFieldChunkIdx(rs.getInt("field_chunk_idx"));
        chunk.setContent(rs.getString("content"));
        chunk.setEmbeddingModel(rs.getString("embedding_model"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            chunk.setCreatedAt(createdAt.toInstant());
        }
        return chunk;
    };

    public ResourceChunkDaoImpl(@Qualifier("registryDataSource") DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    @Transactional
    public void replaceChunks(String resourceId, List<ResourceChunk> chunks) {
        deleteByResourceId(resourceId);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (ResourceChunk chunk : chunks) {
            jdbcTemplate.update(INSERT_SQL, new MapSqlParameterSource()
                    .addValue("resourceId", resourceId)
                    .addValue("chunkIdx", chunk.getChunkIdx())
                    .addValue("fieldName", chunk.getFieldName())
                    .addValue("valueOrdinal", chunk.getValueOrdinal())
                    .addValue("fieldChunkIdx", chunk.getFieldChunkIdx())
                    .addValue("content", chunk.getContent())
                    .addValue("embedding", toVectorLiteral(chunk.getEmbedding()))
                    .addValue("embeddingModel", chunk.getEmbeddingModel()));
        }
    }

    @Override
    @Transactional
    public void deleteByResourceId(String resourceId) {
        jdbcTemplate.update(DELETE_SQL, new MapSqlParameterSource("resourceId", resourceId));
    }

    @Override
    public long countByResourceId(String resourceId) {
        Long count = jdbcTemplate.queryForObject(COUNT_SQL,
                new MapSqlParameterSource("resourceId", resourceId),
                Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public List<ResourceChunk> findByResourceIdOrderByChunkIdxAsc(String resourceId) {
        return jdbcTemplate.query(FIND_BY_RESOURCE_ID_SQL,
                new MapSqlParameterSource("resourceId", resourceId),
                rowMapper);
    }

    private String toVectorLiteral(float[] embedding) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : embedding) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }
}
