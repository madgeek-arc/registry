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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;

import java.util.List;
import java.util.function.Consumer;

public interface ResourceService {
    Resource getResource(String id);

    List<Resource> getResource(ResourceType resourceType);

    void getResourceStream(Consumer<Resource> consumer);

    List<Resource> getResource(ResourceType resourceType, int from, int to);

    List<Resource> getResource(int from, int to);

    List<Resource> getResource();

    Resource addResource(Resource resource) throws ServiceException;

    Resource updateResource(Resource resource) throws ServiceException;

    Resource changeResourceType(Resource resource, ResourceType resourceType);

    void deleteResource(String id);
}

