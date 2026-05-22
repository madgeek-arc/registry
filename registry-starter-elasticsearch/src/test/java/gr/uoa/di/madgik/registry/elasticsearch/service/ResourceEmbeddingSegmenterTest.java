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
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingSegmenter;
import gr.uoa.di.madgik.registry.domain.Segment;
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

class ResourceEmbeddingSegmenterTest {

    @Test
    void textFields_areSplitBeforeSegmentCreation() {
        Resource resource = new Resource();
        resource.setIndexedFields(List.of(mockIndexedField("description", Set.<Object>of("First sentence. Second sentence."))));

        IndexField field = new IndexField();
        field.setName("description");
        field.setLabel("Description");
        field.setType("java.lang.String");
        field.setEmbeddingWeight(2.0f);
        field.setSearchCapabilities(EnumSet.of(SearchCapability.TEXT));

        List<Segment> segments = ResourceEmbeddingSegmenter.segment(resource, List.of(field));

        assertEquals(1, segments.size());
        assertEquals("Description", segments.get(0).getLabel());
        assertEquals(List.of("First sentence.", "Second sentence."), segments.get(0).getValues());
    }

    @Test
    void keywordFields_remainWholeValues() {
        Resource resource = new Resource();
        resource.setIndexedFields(List.of(mockIndexedField("tag", Set.<Object>of("alpha beta. gamma"))));

        IndexField field = new IndexField();
        field.setName("tag");
        field.setLabel("Tag");
        field.setType("java.lang.String");
        field.setEmbeddingWeight(1.0f);
        field.setSearchCapabilities(EnumSet.of(SearchCapability.KEYWORD));

        List<Segment> segments = ResourceEmbeddingSegmenter.segment(resource, List.of(field));

        assertEquals(1, segments.size());
        assertEquals(List.of("alpha beta. gamma"), segments.get(0).getValues());
    }

    @Test
    void nullEmbeddingWeight_isTreatedAsNeutralWeightForLegacyFields() {
        Resource resource = new Resource();
        resource.setIndexedFields(List.of(mockIndexedField("description", Set.<Object>of("Legacy text"))));

        IndexField field = new IndexField();
        field.setName("description");
        field.setLabel("Description");
        field.setType("java.lang.String");
        field.setEmbeddingWeight(null);
        field.setSearchCapabilities(EnumSet.of(SearchCapability.TEXT));

        List<Segment> segments = ResourceEmbeddingSegmenter.segment(resource, List.of(field));

        assertEquals(1, segments.size());
        assertEquals(1.0f, segments.get(0).getWeight());
        assertEquals(List.of("Legacy text"), segments.get(0).getValues());
    }

    @Test
    void nullEmbeddingWeight_onNonStringFieldsDoesNotWarnOrEmbed() {
        Resource resource = new Resource();
        resource.setIndexedFields(List.of(mockIndexedField("year", Set.<Object>of(2024))));

        IndexField field = new IndexField();
        field.setName("year");
        field.setLabel("Year");
        field.setType("java.lang.Integer");
        field.setEmbeddingWeight(null);

        List<Segment> segments = ResourceEmbeddingSegmenter.segment(resource, List.of(field));

        assertEquals(List.of(), segments);
        assertEquals(0.0f, field.getEmbeddingWeight());
    }

    @Test
    void nonStringFields_areExcludedFromSemanticSegments() {
        Resource resource = new Resource();
        resource.setIndexedFields(List.of(mockIndexedField("year", Set.<Object>of(2024))));

        IndexField field = new IndexField();
        field.setName("year");
        field.setLabel("Year");
        field.setType("java.lang.Integer");
        field.setEmbeddingWeight(1.0f);

        List<Segment> segments = ResourceEmbeddingSegmenter.segment(resource, List.of(field));

        assertEquals(List.of(), segments);
    }

    @SuppressWarnings("unchecked")
    private IndexedField<Object> mockIndexedField(String name, Set<Object> values) {
        IndexedField<Object> indexedField = mock(IndexedField.class);
        when(indexedField.getName()).thenReturn(name);
        when(indexedField.getValues()).thenReturn(values);
        return indexedField;
    }
}
