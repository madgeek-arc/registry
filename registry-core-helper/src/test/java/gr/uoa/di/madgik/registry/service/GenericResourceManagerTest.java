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

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers {@code extractPrimaryKeys}'s guard against a primary-key {@link IndexField} that has no
 * value in the create/update payload — previously that {@code null} flowed unchecked into
 * {@link SearchService#searchFields}, crashing the Elasticsearch client with an opaque
 * {@code MissingRequiredPropertyException} (or, on the SQL backend, silently matching nothing and
 * letting a resource with a null-valued primary key get persisted).
 */
class GenericResourceManagerTest {

    private SearchService searchService;
    private ResourceService resourceService;
    private ResourceTypeService resourceTypeService;
    private ParserService parserService;
    private GenericResourceManager manager;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        resourceService = mock(ResourceService.class);
        resourceTypeService = mock(ResourceTypeService.class);
        VersionService versionService = mock(VersionService.class);
        parserService = mock(ParserService.class);
        FacetLabelService facetLabelService = mock(FacetLabelService.class);
        manager = new GenericResourceManager(searchService, resourceService, resourceTypeService,
                versionService, parserService, facetLabelService, null);

        ResourceType resourceType = resourceType("widget", indexField("code", "$.code"));
        when(resourceTypeService.getResourceType("widget")).thenReturn(resourceType);
        when(parserService.serialize(any(), any())).thenReturn("{}");
    }

    @Test
    void addWithMissingPrimaryKeyValueThrowsWithoutSearching() {
        when(parserService.extractValue(anyString(), anyString(), anyString())).thenReturn(null);

        ServiceException e = assertThrows(ServiceException.class, () -> manager.add("widget", new Object()));

        assertTrue(e.getMessage().contains("code"));
        verifyNoInteractions(searchService);
        verify(resourceService, never()).addResource(any());
    }

    @Test
    void addWithPrimaryKeyPresentAndNoDuplicateSucceeds() {
        when(parserService.extractValue(anyString(), anyString(), anyString())).thenReturn("W-1");
        when(searchService.searchFields(eq("widget"), any())).thenReturn(null);

        manager.add("widget", new Object());

        verify(resourceService).addResource(any());
    }

    @Test
    void addWithPrimaryKeyPresentAndDuplicateThrowsAlreadyExists() {
        when(parserService.extractValue(anyString(), anyString(), anyString())).thenReturn("W-1");
        when(searchService.searchFields(eq("widget"), any())).thenReturn(new Resource());

        assertThrows(ResourceAlreadyExistsException.class, () -> manager.add("widget", new Object()));
        verify(resourceService, never()).addResource(any());
    }

    @Test
    void updateWithMissingPrimaryKeyValueThrows() {
        when(parserService.extractValue(anyString(), anyString(), anyString())).thenReturn(null);

        assertThrows(ServiceException.class, () -> manager.update("widget", new Object()));
        verifyNoInteractions(searchService);
    }

    private static ResourceType resourceType(String name, IndexField... indexFields) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        resourceType.setPayloadType("json");
        resourceType.setIndexFields(List.of(indexFields));
        return resourceType;
    }

    private static IndexField indexField(String name, String path) {
        IndexField indexField = new IndexField();
        indexField.setName(name);
        indexField.setPath(path);
        indexField.setPrimaryKey(true);
        return indexField;
    }
}
