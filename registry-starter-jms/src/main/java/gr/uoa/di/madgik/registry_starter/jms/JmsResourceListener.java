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

package gr.uoa.di.madgik.registry_starter.jms;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.jms.BaseResourceJms;
import gr.uoa.di.madgik.registry.domain.jms.ResourceJmsCreated;
import gr.uoa.di.madgik.registry.domain.jms.ResourceJmsDeleted;
import gr.uoa.di.madgik.registry.domain.jms.ResourceJmsUpdated;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import gr.uoa.di.madgik.registry.monitor.ResourceTypeListener;
import gr.uoa.di.madgik.registry_starter.autoconfigure.JmsAutoConfiguration;
import gr.uoa.di.madgik.registry_starter.autoconfigure.JmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.jms.core.JmsTemplate;

@AutoConfigureAfter(JmsAutoConfiguration.ConfigureJms.class)
public class JmsResourceListener implements ResourceListener, ResourceTypeListener {

    private static final Logger logger = LoggerFactory.getLogger(JmsResourceListener.class);

    private final JmsProperties jmsProperties;
    private final JmsTemplate jmsTopicTemplate;

    public JmsResourceListener(JmsProperties jmsProperties, JmsTemplate jmsTopicTemplate) {
        this.jmsProperties = jmsProperties;
        this.jmsTopicTemplate = jmsTopicTemplate;
    }

    @Override
    public void resourceAdded(Resource resource) {
        String destination = String.format("%s.%s.create", jmsProperties.getPrefix(), resource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsCreated(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Added new resource at: {}", destination);
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        String destination = String.format("%s.%s.update", jmsProperties.getPrefix(), newResource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsUpdated(newResource, previousResource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Updated resource at: {}", destination);
    }

    @Override
    public void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType) {
        String destination = String.format("%s.%s.delete", jmsProperties.getPrefix(), previousResourceType.getName());
        BaseResourceJms jmsResource = new ResourceJmsDeleted(resource); // FIXME: needs to get previous resource
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Deleted resource at: {}", destination);

        destination = String.format("%s.%s.create", jmsProperties.getPrefix(), resourceType.getName());
        jmsResource = new ResourceJmsCreated(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Added new resource at: {}", destination);
    }

    @Override
    public void resourceDeleted(Resource resource) {
        String destination = String.format("%s.%s.delete", jmsProperties.getPrefix(), resource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsDeleted(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Deleted resource at: {}", destination);
    }

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        logger.warn("JMS is NOT notified for the insertion of resource type '{}'", resourceType.getName());
    }

    @Override
    public void resourceTypeDelete(String name) {
        logger.warn("JMS is NOT notified for the deletion of resource type '{}'", name);
    }

}
