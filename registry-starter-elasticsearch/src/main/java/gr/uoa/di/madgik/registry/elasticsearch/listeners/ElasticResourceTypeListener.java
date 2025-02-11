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

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.monitor.ResourceTypeListener;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticResourceTypeListener implements ResourceTypeListener {

    private final IndexOperationsService indexOperationsService;
    private Logger logger = LoggerFactory.getLogger(ElasticResourceTypeListener.class);

    public ElasticResourceTypeListener(IndexOperationsService indexOperationsService) {
        this.indexOperationsService = indexOperationsService;
    }

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        indexOperationsService.createIndex(resourceType);
    }

    @Override
    public void resourceTypeDelete(String name) {
        indexOperationsService.deleteIndex(name);
    }

}
