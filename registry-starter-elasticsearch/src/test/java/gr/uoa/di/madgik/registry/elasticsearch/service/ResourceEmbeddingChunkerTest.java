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

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunk;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunker;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceEmbeddingChunkerTest {

    @Test
    void textFields_areSplitIntoChunks_withLabelPrefixedEmbeddingText() {
        Resource resource = new Resource();
        resource.setIndexedFields(List.of(mockIndexedField("description", Set.<Object>of("First sentence. Second sentence."))));

        IndexField field = new IndexField();
        field.setName("description");
        field.setLabel("Description");
        field.setType("java.lang.String");
        field.setSearchCapabilities(EnumSet.of(SearchCapability.TEXT));

        List<ResourceEmbeddingChunk> chunks = ResourceEmbeddingChunker.chunk(resource, List.of(field));

        assertEquals(2, chunks.size());
        assertEquals(0, chunks.get(0).chunkIdx());
        assertEquals(0, chunks.get(0).valueOrdinal());
        assertEquals(0, chunks.get(0).fieldChunkIdx());
        assertEquals("First sentence.", chunks.get(0).content());
        assertEquals("Description: First sentence.", chunks.get(0).embeddingText());
        assertEquals(1, chunks.get(1).fieldChunkIdx());
        assertEquals("Second sentence.", chunks.get(1).content());
        assertEquals("Description: Second sentence.", chunks.get(1).embeddingText());
    }

    @SuppressWarnings("unchecked")
    private IndexedField<Object> mockIndexedField(String name, Set<Object> values) {
        IndexedField<Object> indexedField = mock(IndexedField.class);
        when(indexedField.getName()).thenReturn(name);
        when(indexedField.getValues()).thenReturn(values);
        return indexedField;
    }
}
