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

package gr.uoa.di.madgik.registry.domain;

import java.time.Instant;
public class ResourceChunk {
    private Long id;
    private String resourceId;
    private int chunkIdx;
    private String fieldName;
    private int valueOrdinal;
    private int fieldChunkIdx;
    private String content;
    private float[] embedding;
    private String embeddingModel;
    private Instant createdAt = Instant.now();

    public ResourceChunk() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getChunkIdx() {
        return chunkIdx;
    }

    public void setChunkIdx(int chunkIdx) {
        this.chunkIdx = chunkIdx;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public int getValueOrdinal() {
        return valueOrdinal;
    }

    public void setValueOrdinal(int valueOrdinal) {
        this.valueOrdinal = valueOrdinal;
    }

    public int getFieldChunkIdx() {
        return fieldChunkIdx;
    }

    public void setFieldChunkIdx(int fieldChunkIdx) {
        this.fieldChunkIdx = fieldChunkIdx;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
}
