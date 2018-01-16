package eu.openminted.registry.core.jms;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.jms.BaseResourceJms;
import eu.openminted.registry.core.domain.jms.ResourceJmsCreated;
import eu.openminted.registry.core.domain.jms.ResourceJmsDeleted;
import eu.openminted.registry.core.domain.jms.ResourceJmsUpdated;
import eu.openminted.registry.core.monitor.ResourceListener;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class JmsResourceListener implements ResourceListener, ResourceTypeListener {

    @Autowired
    JmsTemplate jmsTopicTemplate;

    private static Logger logger = Logger.getLogger(JmsResourceListener.class);

    @Override
    public void resourceAdded(Resource resource) {
        String destination = resource.getResourceType().getName() + ".create";
        BaseResourceJms jmsResource = new ResourceJmsCreated(resource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.info("Added new resource at " + destination);
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        String destination = newResource.getResourceType().getName() + ".update";
        BaseResourceJms jmsResource = new ResourceJmsUpdated(newResource,previousResource);
        jmsTopicTemplate.convertAndSend(destination, jmsResource);
        logger.info("Updated resource at " + destination);
    }

    @Override
    public void resourceDeleted(Resource resource) {
        String destination = resource.getResourceType().getName() + ".delete";
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
