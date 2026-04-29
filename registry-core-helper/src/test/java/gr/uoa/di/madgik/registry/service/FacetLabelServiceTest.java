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

import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.Value;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacetLabelServiceTest {

    @Test
    void enrichFacetLabels_leavesLabelUnsetForNonKeywordFields() {
        SearchService searchService = mock(SearchService.class);
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        FacetLabelService facetLabelService = new FacetLabelService(searchService, resourceTypeService);
        IndexField description = indexField("description", SearchCapability.TEXT);
        description.setRelatedResourceType("category");
        description.setRelatedResourceTypeField("name");
        Facet facet = new Facet("description", "Description",
                List.of(new Value("free text bucket", 2)));
        when(resourceTypeService.getResourceTypeIndexFields("service")).thenReturn(Set.of(description));

        facetLabelService.enrichFacetLabels(List.of(facet), "service");

        assertNull(facet.getValues().getFirst().getLabel());
        verify(searchService, never()).getLabels(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void enrichFacetLabels_looksUpLabelsForKeywordFields() {
        SearchService searchService = mock(SearchService.class);
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        FacetLabelService facetLabelService = new FacetLabelService(searchService, resourceTypeService);
        IndexField category = indexField("category", SearchCapability.KEYWORD);
        category.setRelatedResourceType("category");
        category.setRelatedResourceTypeField("name");
        IndexField categoryId = indexField("id", SearchCapability.KEYWORD);
        categoryId.setPrimaryKey(true);
        IndexField categoryName = indexField("name", SearchCapability.TEXT);
        Facet facet = new Facet("category", "Category", List.of(new Value("cat-1", 1)));
        when(resourceTypeService.getResourceTypeIndexFields("service")).thenReturn(Set.of(category));
        when(resourceTypeService.getResourceTypeIndexFields("category")).thenReturn(Set.of(categoryId, categoryName));
        when(searchService.getLabels("category", "id", List.of("cat-1"), "name"))
                .thenReturn(Map.of("cat-1", "Category One"));

        facetLabelService.enrichFacetLabels(List.of(facet), "service");

        assertEquals("Category One", facet.getValues().getFirst().getLabel());
    }

    @Test
    void enrichFacetLabels_batchesLookupsByRelatedTypeAndLabelField() {
        SearchService searchService = mock(SearchService.class);
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        FacetLabelService facetLabelService = new FacetLabelService(searchService, resourceTypeService);
        IndexField categoryByName = indexField("category", SearchCapability.KEYWORD);
        categoryByName.setRelatedResourceType("category");
        categoryByName.setRelatedResourceTypeField("name");
        IndexField categoryByTitle = indexField("categoryTitle", SearchCapability.KEYWORD);
        categoryByTitle.setRelatedResourceType("category");
        categoryByTitle.setRelatedResourceTypeField("title");
        IndexField categoryId = indexField("id", SearchCapability.KEYWORD);
        categoryId.setPrimaryKey(true);
        IndexField categoryName = indexField("name", SearchCapability.TEXT);
        IndexField categoryTitle = indexField("title", SearchCapability.TEXT);
        Facet nameFacet = new Facet("category", "Category", List.of(new Value("cat-1", 1)));
        Facet titleFacet = new Facet("categoryTitle", "Category title", List.of(new Value("cat-2", 1)));
        when(resourceTypeService.getResourceTypeIndexFields("service"))
                .thenReturn(Set.of(categoryByName, categoryByTitle));
        when(resourceTypeService.getResourceTypeIndexFields("category"))
                .thenReturn(Set.of(categoryId, categoryName, categoryTitle));
        when(searchService.getLabels("category", "id", List.of("cat-1"), "name"))
                .thenReturn(Map.of("cat-1", "Category One"));
        when(searchService.getLabels("category", "id", List.of("cat-2"), "title"))
                .thenReturn(Map.of("cat-2", "Category Two"));

        facetLabelService.enrichFacetLabels(List.of(nameFacet, titleFacet), "service");

        assertEquals("Category One", nameFacet.getValues().getFirst().getLabel());
        assertEquals("Category Two", titleFacet.getValues().getFirst().getLabel());
        verify(searchService).getLabels("category", "id", List.of("cat-1"), "name");
        verify(searchService).getLabels("category", "id", List.of("cat-2"), "title");
    }

    private static IndexField indexField(String name, SearchCapability capability) {
        IndexField indexField = new IndexField();
        indexField.setName(name);
        indexField.setSearchCapabilities(EnumSet.of(capability));
        return indexField;
    }
}
