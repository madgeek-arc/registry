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

package gr.uoa.di.madgik.registry.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.util.ObjectBuilder;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticOperationsServiceAddBulkTest {

    @Test
    void addBulk_splitsLargeResourceListsIntoSubBatches() throws Exception {
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        when(resourceTypeService.getResourceTypeIndexFields(anyString())).thenReturn(Collections.emptySet());

        ElasticsearchClient client = mock(ElasticsearchClient.class);

        ElasticOperationsService service = new ElasticOperationsService(
                resourceTypeService,
                mock(ResourceService.class),
                client,
                mock(EmbeddingService.class),
                new ObjectMapper(),
                2
        );

        service.addBulk(resources(5));

        verify(client, times(3)).bulk(ArgumentMatchers.<Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>>any());
    }

    @Test
    void addBulk_issuesSingleRequest_whenResourceCountFitsInOneBatch() throws Exception {
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        when(resourceTypeService.getResourceTypeIndexFields(anyString())).thenReturn(Collections.emptySet());

        ElasticsearchClient client = mock(ElasticsearchClient.class);

        ElasticOperationsService service = new ElasticOperationsService(
                resourceTypeService,
                mock(ResourceService.class),
                client,
                mock(EmbeddingService.class),
                new ObjectMapper(),
                100
        );

        service.addBulk(resources(5));

        verify(client, times(1)).bulk(ArgumentMatchers.<Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>>any());
    }

    private static List<Resource> resources(int count) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName("test_resource_type");

        List<Resource> resources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Resource resource = new Resource();
            resource.setId("id-" + i);
            resource.setResourceType(resourceType);
            resource.setPayload("<payload/>");
            resource.setPayloadFormat("xml");
            resource.setVersion("1");
            resource.setModificationDate(Instant.now());
            resource.setCreatedBy("system");
            resource.setModifiedBy("system");
            resources.add(resource);
        }
        return resources;
    }
}
