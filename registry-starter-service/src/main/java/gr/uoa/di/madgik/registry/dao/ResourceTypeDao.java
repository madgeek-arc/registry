/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

    List<ResourceType> getAllResourceType();

    List<ResourceType> getAllResourceTypeByAlias(String alias);

    List<ResourceType> getAllResourceType(int from, int to);

    void addResourceType(ResourceType resource);

    Set<IndexField> getResourceTypeIndexFields(String name);

    void deleteResourceType(String resourceType);

}
