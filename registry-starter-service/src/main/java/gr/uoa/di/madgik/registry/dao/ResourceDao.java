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

package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * Persistence access for registry {@link Resource} entities and their derived projections.
 */
public interface ResourceDao {

    /**
     * Returns the resource with the given identifier, or {@code null} when it does not exist.
     *
     * @param id the resource identifier
     * @return the matching resource, or {@code null} if no resource exists with that id
     */
    Resource getResource(String id);

    /**
     * Returns resources modified strictly after {@code date}, optionally filtered by resource type name.
     *
     * @param date the lower bound for {@code modificationDate}
     * @param resourceType the resource type name to filter by; implementations may treat blank values as no filter
     * @return all matching resources
     */
    List<Resource> getModifiedSince(Instant date, String resourceType);

    /**
     * Returns resources modified strictly after {@code date} across all resource types.
     *
     * @param date the lower bound for {@code modificationDate}
     * @return all matching resources
     */
    List<Resource> getModifiedSince(Instant date);

    /**
     * Returns resources created strictly after {@code date} across all resource types.
     *
     * @param date the lower bound for {@code creationDate}
     * @return all matching resources
     */
    List<Resource> getCreatedSince(Instant date);

    /**
     * Returns resources created strictly after {@code date} for the given resource type name.
     *
     * @param date the lower bound for {@code creationDate}
     * @param resourceType the resource type name to filter by
     * @return all matching resources
     */
    List<Resource> getCreatedSince(Instant date, String resourceType);

    /**
     * Returns all resources that belong to the provided resource type.
     *
     * @param resourceType the resource type entity to match
     * @return all resources of that type
     */
    List<Resource> getResource(ResourceType resourceType);

    /**
     * Returns the total number of persisted resources for the given resource type.
     *
     * @param resourceType the resource type entity to count resources for
     * @return the number of matching resources
     */
    Long getTotal(ResourceType resourceType);

    /**
     * Streams all resources for bulk-processing use cases.
     *
     * @return a stream over all persisted resources
     */
    Stream<Resource> getResourceStream();

    /**
     * Returns a paged slice of resources for the given resource type.
     *
     * @param resourceType the resource type entity to filter by; {@code null} means all resource types
     * @param from the zero-based starting offset
     * @param to the inclusive upper bound or implementation-defined page limit sentinel
     * @return the requested slice of resources
     */
    List<Resource> getResource(ResourceType resourceType, int from, int to);

    /**
     * Returns a paged slice of resources across all resource types.
     *
     * @param from the zero-based starting offset
     * @param to the inclusive upper bound or implementation-defined page limit sentinel
     * @return the requested slice of resources
     */
    List<Resource> getResource(int from, int to);

    /**
     * Returns all persisted resources.
     *
     * @return all resources
     */
    List<Resource> getResource();

    /**
     * Persists a new resource.
     *
     * @param resource the resource to insert
     * @return the persisted resource instance
     */
    Resource addResource(Resource resource);

    /**
     * Persists a business-level resource update and refreshes its modification timestamp.
     *
     * @param resource the resource state to persist
     * @return the merged resource instance with an updated modification timestamp
     */
    Resource updateResource(Resource resource);

    /**
     * Merges resource state without applying DAO-level timestamp mutations.
     *
     * @param resource the resource state to merge
     * @return the merged resource instance without forcing a new modification timestamp
     */
    Resource mergeResource(Resource resource);

    /**
     * Deletes the given resource entity.
     *
     * @param id the managed resource entity to delete
     */
    void deleteResource(Resource id);

}
