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

package gr.uoa.di.madgik.registry.elasticsearch.listeners;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;

public class ElasticResourceListener implements ResourceListener {

    private final IndexOperationsService indexOperationsService;

    public ElasticResourceListener(IndexOperationsService indexOperationsService) {
        this.indexOperationsService = indexOperationsService;
    }

    @Override
    public void resourceAdded(Resource resource) {
        indexOperationsService.add(resource);
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        indexOperationsService.update(previousResource, newResource);
    }

    @Override
    public void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType) {
        indexOperationsService.delete(resource.getId(), previousResourceType.getName());
        indexOperationsService.add(resource);
    }

    @Override
    public void resourceDeleted(Resource resource) {
        indexOperationsService.delete(resource);
    }

}
