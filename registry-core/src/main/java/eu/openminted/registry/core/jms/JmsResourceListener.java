package eu.openminted.registry.core.jms;

import eu.openminted.registry.core.configuration.JmsConfiguration;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.jms.BaseResourceJms;
import eu.openminted.registry.core.domain.jms.ResourceJmsCreated;
import eu.openminted.registry.core.domain.jms.ResourceJmsDeleted;
import eu.openminted.registry.core.domain.jms.ResourceJmsUpdated;
import eu.openminted.registry.core.monitor.ResourceListener;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class JmsResourceListener implements ResourceListener, ResourceTypeListener {

    private static final Logger logger = LoggerFactory.getLogger(JmsResourceListener.class);

    private final JmsConfiguration jmsConfiguration;
    private final JmsTemplate jmsTopicTemplate;

    public JmsResourceListener(JmsConfiguration jmsConfiguration, JmsTemplate jmsTopicTemplate) {
        this.jmsConfiguration = jmsConfiguration;
        this.jmsTopicTemplate = jmsTopicTemplate;
    }

    @Override
    public void resourceAdded(Resource resource) {
        String destination = String.format("%s.%s.create", jmsConfiguration.getJmsPrefix(), resource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsCreated(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Added new resource at: {}", destination);
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        String destination = String.format("%s.%s.update", jmsConfiguration.getJmsPrefix(), newResource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsUpdated(newResource, previousResource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Updated resource at: {}", destination);
    }

    @Override
    public void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType) {
        String destination = String.format("%s.%s.delete", jmsConfiguration.getJmsPrefix(), previousResourceType.getName());
        BaseResourceJms jmsResource = new ResourceJmsDeleted(resource); // FIXME: needs to get previous resource
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Deleted resource at: {}", destination);

        destination = String.format("%s.%s.create", jmsConfiguration.getJmsPrefix(), resourceType.getName());
        jmsResource = new ResourceJmsCreated(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.debug("Added new resource at: {}", destination);
    }

    @Override
    public void resourceDeleted(Resource resource) {
        String destination = String.format("%s.%s.delete", jmsConfiguration.getJmsPrefix(), resource.getResourceType().getName());
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
