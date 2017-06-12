package eu.openminted.registry.core.jms.listeners;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.jms.service.JMSService;
import eu.openminted.registry.core.monitor.ResourceListener;
import net.minidev.json.JSONObject;

import javax.jms.JMSException;

@Component
public class JmsResourceListener implements ResourceListener, ResourceTypeListener {

	@Autowired
	JMSService jmsService;
	@Autowired
	private Environment environment;

	private static Logger logger = Logger.getLogger(JmsResourceListener.class);
	
	@Override
	public void resourceAdded(Resource resource) {
		String topic = getTopic(resource, "create");
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("message", "New resource");
		jsonObject.put("resourceId", resource.getId());
		jsonObject.put("resourceType", resource.getResourceType());
		jsonObject.put("resource", resource.getPayload());

		try {
			jmsService.publishMessage(topic, jsonObject.toString());
		} catch (JMSException e) {
			logger.error("Error publishing message", e);
		}
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		String topic = getTopic(previousResource, "update");
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("message", "Resource updated");
		jsonObject.put("resourceId", newResource.getId());
		jsonObject.put("resourceType", newResource.getResourceType());
		jsonObject.put("previous", previousResource.getPayload());
		jsonObject.put("new", newResource.getPayload());

		try {
			jmsService.publishMessage(topic, jsonObject.toString());
		} catch (JMSException e) {
			logger.error("Error publishing message", e);
		}
	}

	@Override
	public void resourceDeleted(Resource resource) {
		String topic = getTopic(resource, "delete");
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("message", "Resource deleted.");
		jsonObject.put("resourceId", resource.getId());
		jsonObject.put("resourceType", resource.getResourceType());


		try {
			jmsService.publishMessage(topic, jsonObject.toString());
		} catch (JMSException e) {
			logger.error("Error publishing message", e);
		}
	}

	@Override
	public void resourceTypeAdded(ResourceType resourceType) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("message", "A new resource type has been added");
		jsonObject.put("resourceType", resourceType.getName());

		try {
			jmsService.publishMessage("General", jsonObject.toString());
			jmsService.createTopic(getTopic(resourceType.getName(), "create"));
			jmsService.createTopic(getTopic(resourceType.getName(), "update"));
			jmsService.createTopic(getTopic(resourceType.getName(), "delete"));
		} catch (JMSException e) {
			logger.error("Error publishing message/creating topic", e);
		}

	}

	private String getTopic(Resource resource, String action) {
		return getTopic(resource.getResourceType(), action);
	}

	private String getTopic(String resourceType, String action) {
		return environment.getProperty("jms.prefix") + "." + resourceType + "." + action;
	}

}
