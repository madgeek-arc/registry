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

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface ResourceTypeService {
    Schema getSchema(String id);

    ResourceType getResourceType(String name);

    List<ResourceType> getAllResourceType();

    List<ResourceType> getAllResourceTypeByAlias(String alias);

    List<ResourceType> getAllResourceType(int from, int to);

    ResourceType addResourceType(ResourceType resourceType) throws ServiceException;

    ResourceType updateResourceType(ResourceType resourceType) throws ServiceException;

    Set<IndexField> getResourceTypeIndexFields(String name);

    /**
     * Returns a map of {@link IndexField#getName()} → {@link IndexField#getLabel()} for fields
     * that have a non-null label, for the given resource type.
     */
    default Map<String, String> getIndexFieldLabels(String name) {
        return getResourceTypeIndexFields(name).stream()
                .filter(f -> f.getLabel() != null)
                .sorted(Comparator
                        .comparing((IndexField f) -> resourceTypeName(f), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(IndexField::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toMap(
                        IndexField::getName,
                        IndexField::getLabel,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ));
    }

    void deleteResourceType(String name);

    private static String resourceTypeName(IndexField indexField) {
        return indexField.getResourceType() == null ? null : indexField.getResourceType().getName();
    }
}
