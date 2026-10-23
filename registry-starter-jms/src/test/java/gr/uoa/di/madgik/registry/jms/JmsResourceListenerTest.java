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

package gr.uoa.di.madgik.registry.jms;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.jms.BaseResourceJms;
import gr.uoa.di.madgik.registry.domain.jms.ResourceJmsCreated;
import gr.uoa.di.madgik.registry.domain.jms.ResourceJmsDeleted;
import gr.uoa.di.madgik.registry.domain.jms.ResourceJmsUpdated;
import gr.uoa.di.madgik.registry.jms.autoconfigure.JmsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JmsResourceListenerTest {

    private JmsTemplate jmsTemplate;
    private JmsResourceListener listener;

    @BeforeEach
    void setUp() {
        JmsProperties properties = new JmsProperties();
        properties.setPrefix("test");
        jmsTemplate = mock(JmsTemplate.class);
        listener = new JmsResourceListener(properties, jmsTemplate);
    }

    @Test
    void resourceAddedPublishesCreateEventOnTypeTopic() {
        listener.resourceAdded(resource("r1", "service", "{\"v\":1}"));

        BaseResourceJms message = sent("test.service.create");
        assertThat(message).isInstanceOf(ResourceJmsCreated.class);
        assertThat(message.getResourceId()).isEqualTo("r1");
        assertThat(message.getResourceType()).isEqualTo("service");
        assertThat(message.getResource()).isEqualTo("{\"v\":1}");
        assertThat(message.getPayloadFormat()).isEqualTo("json");
    }

    @Test
    void resourceUpdatedPublishesNewAndPreviousPayloads() {
        listener.resourceUpdated(resource("r1", "service", "{\"v\":1}"), resource("r1", "service", "{\"v\":2}"));

        BaseResourceJms message = sent("test.service.update");
        assertThat(message).isInstanceOf(ResourceJmsUpdated.class);
        assertThat(message.getResource()).isEqualTo("{\"v\":2}");
        assertThat(((ResourceJmsUpdated) message).getPrevious()).isEqualTo("{\"v\":1}");
    }

    @Test
    void resourceDeletedPublishesDeleteEvent() {
        listener.resourceDeleted(resource("r1", "service", "{\"v\":1}"));

        BaseResourceJms message = sent("test.service.delete");
        assertThat(message).isInstanceOf(ResourceJmsDeleted.class);
        assertThat(message.getResourceId()).isEqualTo("r1");
    }

    @Test
    void resourceChangedTypePublishesDeleteOfPreviousResourceThenCreateOfNewResource() {
        ResourceType oldType = resourceType("service");
        ResourceType newType = resourceType("datasource");
        Resource previous = resource("r1", oldType, "{\"v\":1}");
        Resource current = resource("r1", newType, "{\"v\":2}");

        listener.resourceChangedType(previous, current, oldType, newType);

        ArgumentCaptor<String> destinations = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> messages = ArgumentCaptor.forClass(Object.class);
        verify(jmsTemplate, times(2)).convertAndSend(destinations.capture(), messages.capture());
        assertThat(destinations.getAllValues()).containsExactly("test.service.delete", "test.datasource.create");

        List<Object> sent = messages.getAllValues();
        assertThat(sent.get(0)).isInstanceOf(ResourceJmsDeleted.class);
        assertThat(((BaseResourceJms) sent.get(0)).getResourceType()).isEqualTo("service");
        assertThat(((BaseResourceJms) sent.get(0)).getResource()).isEqualTo("{\"v\":1}");
        assertThat(sent.get(1)).isInstanceOf(ResourceJmsCreated.class);
        assertThat(((BaseResourceJms) sent.get(1)).getResourceType()).isEqualTo("datasource");
        assertThat(((BaseResourceJms) sent.get(1)).getResource()).isEqualTo("{\"v\":2}");
    }

    @Test
    void resourceTypeChangesAreNotPublished() {
        listener.resourceTypeAdded(resourceType("service"));
        listener.resourceTypeDelete("service");

        verify(jmsTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    private BaseResourceJms sent(String destination) {
        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(jmsTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(destination), message.capture());
        return (BaseResourceJms) message.getValue();
    }

    private static ResourceType resourceType(String name) {
        ResourceType type = new ResourceType();
        type.setName(name);
        return type;
    }

    private static Resource resource(String id, String typeName, String payload) {
        return resource(id, resourceType(typeName), payload);
    }

    private static Resource resource(String id, ResourceType type, String payload) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setResourceType(type);
        resource.setPayload(payload);
        resource.setPayloadFormat("json");
        return resource;
    }
}
