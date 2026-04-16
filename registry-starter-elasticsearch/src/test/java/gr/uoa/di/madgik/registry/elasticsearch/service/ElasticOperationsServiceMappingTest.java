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

package gr.uoa.di.madgik.registry.elasticsearch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ElasticOperationsServiceMappingTest {

    @Test
    void embeddingField_isIndexedForCosineKnn() throws Exception {
        ElasticOperationsService service = new ElasticOperationsService(
                mock(ResourceTypeService.class),
                mock(ResourceService.class),
                mock(co.elastic.clients.elasticsearch.ElasticsearchClient.class),
                mock(EmbeddingService.class),
                new ObjectMapper()
        );

        Method createMappingAsMap = ElasticOperationsService.class
                .getDeclaredMethod("createMappingAsMap", List.class);
        createMappingAsMap.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> mapping = (Map<String, Object>) createMappingAsMap.invoke(service, Collections.<IndexField>emptyList());
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> embedding = (Map<String, Object>) properties.get("embedding");
        @SuppressWarnings("unchecked")
        Map<String, Object> chunkEmbeddings = (Map<String, Object>) properties.get("chunk_embeddings");
        @SuppressWarnings("unchecked")
        Map<String, Object> chunkEmbeddingProperties = (Map<String, Object>) chunkEmbeddings.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> chunkField = (Map<String, Object>) chunkEmbeddingProperties.get("field");
        @SuppressWarnings("unchecked")
        Map<String, Object> chunkValueOrdinal = (Map<String, Object>) chunkEmbeddingProperties.get("value_ordinal");
        @SuppressWarnings("unchecked")
        Map<String, Object> chunkFieldChunkIdx = (Map<String, Object>) chunkEmbeddingProperties.get("field_chunk_idx");
        @SuppressWarnings("unchecked")
        Map<String, Object> chunkEmbedding = (Map<String, Object>) chunkEmbeddingProperties.get("embedding");
        @SuppressWarnings("unchecked")
        Map<String, Object> chunkContent = (Map<String, Object>) chunkEmbeddingProperties.get("content");

        assertEquals("dense_vector", embedding.get("type"));
        assertEquals(Boolean.TRUE, embedding.get("index"));
        assertEquals("cosine", embedding.get("similarity"));
        assertTrue(((Number) embedding.get("dims")).intValue() > 0);
        assertEquals("nested", chunkEmbeddings.get("type"));
        assertEquals("keyword", chunkField.get("type"));
        assertEquals("integer", chunkValueOrdinal.get("type"));
        assertEquals("integer", chunkFieldChunkIdx.get("type"));
        assertEquals("dense_vector", chunkEmbedding.get("type"));
        assertEquals(Boolean.TRUE, chunkEmbedding.get("index"));
        assertEquals("cosine", chunkEmbedding.get("similarity"));
        assertEquals("text", chunkContent.get("type"));
    }
}
