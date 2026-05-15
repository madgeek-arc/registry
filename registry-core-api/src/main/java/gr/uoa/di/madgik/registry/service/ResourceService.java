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

import java.util.List;
import java.util.function.Consumer;

/**
 * Business service for creating, retrieving, updating, and deleting registry resources.
 */
public interface ResourceService {

    /**
     * Returns the resource with the given identifier.
     *
     * @param id the resource identifier
     * @return the matching resource, or {@code null} when no resource exists with that id
     */
    Resource getResource(String id);

    /**
     * Returns the resource with associations required by indexing and embedding workflows initialized.
     *
     * <p>Implementations should initialize the extracted indexed fields and their values while the
     * persistence context is active so asynchronous indexers can read them without triggering lazy
     * loading failures.</p>
     *
     * @param id the resource identifier
     * @return the matching resource prepared for indexing, or {@code null} when no resource exists with that id
     */
    Resource getResourceForIndexing(String id);

    /**
     * Returns all resources that belong to the given resource type.
     *
     * @param resourceType the resource type to filter by
     * @return resources of the provided type
     */
    List<Resource> getResource(ResourceType resourceType);

    /**
     * Returns the number of resources that belong to the given resource type.
     *
     * @param resourceType the resource type to count
     * @return the number of resources of the provided type
     */
    Long getTotal(ResourceType resourceType);

    /**
     * Streams all resources to the provided consumer.
     *
     * @param consumer callback invoked for each resource
     */
    void getResourceStream(Consumer<Resource> consumer);

    /**
     * Returns a paged slice of resources for the given resource type.
     *
     * @param resourceType the resource type to filter by; implementations may treat {@code null} as all types
     * @param from the zero-based starting offset
     * @param to the upper bound or page-size sentinel defined by the implementation
     * @return the requested resource slice
     */
    List<Resource> getResource(ResourceType resourceType, int from, int to);

    /**
     * Returns a paged slice of resources across all resource types.
     *
     * @param from the zero-based starting offset
     * @param to the upper bound or page-size sentinel defined by the implementation
     * @return the requested resource slice
     */
    List<Resource> getResource(int from, int to);

    /**
     * Returns all resources.
     *
     * @return all persisted resources
     */
    List<Resource> getResource();

    /**
     * Creates a new resource.
     *
     * @param resource the resource to create
     * @return the persisted resource
     * @throws ServiceException when validation, extraction, or persistence fails
     */
    Resource addResource(Resource resource) throws ServiceException;

    /**
     * Updates an existing resource.
     *
     * @param resource the resource state to apply
     * @return the updated resource
     * @throws ServiceException when the resource is invalid, missing, or cannot be persisted
     */
    Resource updateResource(Resource resource) throws ServiceException;

    /**
     * Changes the resource type of an existing resource.
     *
     * @param resource the resource to move
     * @param resourceType the target resource type
     * @return the updated resource
     */
    Resource changeResourceType(Resource resource, ResourceType resourceType);

    /**
     * Deletes the resource with the given identifier.
     *
     * @param id the resource identifier
     */
    void deleteResource(String id);
}
