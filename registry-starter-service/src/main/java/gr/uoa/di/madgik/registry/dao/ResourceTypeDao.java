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

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.List;
import java.util.Set;

public interface ResourceTypeDao {

    ResourceType getResourceType(String name);

    /**
     * Builds a detached snapshot of the resource type's last-committed definition (schema,
     * schemaUrl, payloadType, indexMapperClass, aliases, properties, and index fields), reading
     * every part of it via queries that bypass the persistence context, so pending in-memory
     * changes on an already-managed instance for the same name are never reflected in the result.
     * Intended for {@link gr.uoa.di.madgik.registry.service.ResourceTypeChangeDetector#hasSameDefinition}
     * comparisons that need the true previous definition even when the caller passed in an entity
     * it already fetched and mutated in place.
     *
     * @param name the resource type name
     * @return a detached {@link ResourceType} carrying only the definition fields, or {@code null}
     * if no resource type exists with that name
     */
    ResourceType getPersistedSnapshot(String name);

    List<ResourceType> getAllResourceType();

    List<ResourceType> getAllResourceTypeByAlias(String alias);

    List<ResourceType> getAllResourceType(int from, int to);

    void addResourceType(ResourceType resource);

    ResourceType updateResourceType(ResourceType resourceType);

    Set<IndexField> getResourceTypeIndexFields(String name);

    void deleteResourceType(String resourceType);

}
