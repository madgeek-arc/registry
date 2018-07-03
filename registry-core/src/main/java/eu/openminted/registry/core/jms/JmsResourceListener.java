package eu.openminted.registry.core.jms;

import eu.openminted.registry.core.configuration.JmsConfiguration;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.domain.jms.BaseResourceJms;
import eu.openminted.registry.core.domain.jms.ResourceJmsCreated;
import eu.openminted.registry.core.domain.jms.ResourceJmsDeleted;
import eu.openminted.registry.core.domain.jms.ResourceJmsUpdated;
import eu.openminted.registry.core.monitor.ResourceListener;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class JmsResourceListener implements ResourceListener, ResourceTypeListener {

    @Autowired
    JmsTemplate jmsTopicTemplate;

    @Autowired
    JmsConfiguration jmsConfiguration;



    private static Logger logger = LogManager.getLogger(JmsResourceListener.class);

    @Override
    public void resourceAdded(Resource resource) {
        String destination = String.format("%s.%s.create",jmsConfiguration.getJmsPrefix(),resource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsCreated(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.info("Added new resource at " + destination);
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        String destination = String.format("%s.%s.update",jmsConfiguration.getJmsPrefix(),newResource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsUpdated(newResource,previousResource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.info("Updated resource at " + destination);
    }

    @Override
    public void resourceDeleted(Resource resource) {
        String destination = String.format("%s.%s.delete",jmsConfiguration.getJmsPrefix(),resource.getResourceType().getName());
        BaseResourceJms jmsResource = new ResourceJmsDeleted(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.info("Deleted resource at " + destination);
    }

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        logger.warn("JMS is NOT notified for the insertion of resource type " + resourceType.getName());
    }

    @Override
    public void resourceTypeDelete(String name) {
        logger.warn("JMS is NOT notified for the deletion of resource type " + name);
    }

}
