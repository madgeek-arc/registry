package eu.openminted.registry.core.jms.listeners;

import org.springframework.beans.factory.annotation.Autowired;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.jms.service.JMSService;
import eu.openminted.registry.core.monitor.ResourceListener;
import net.minidev.json.JSONObject;

public class JmsResourceListener implements ResourceListener {

	@Autowired
	JMSService jmsService;
	
	@Override
	public void resourceAdded(Resource resource) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("message", "New resource added to "+resource.getResourceType());
		jmsService.publishMessage(resource.getResourceType()+"-create", jsonObject.toString());
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("message", "Resource with id:"+previousResource.getId()+" updated old-payload:"+previousResource.getPayload()+" new-payload:"+newResource.getPayload());
		jmsService.publishMessage(previousResource.getResourceType()+"-update", jsonObject.toString());
	}

	@Override
	public void resourceDeleted(Resource resource) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("message", "Resource with id:"+resource.getId() +" has been deleted.");
		jmsService.publishMessage(resource.getResourceType()+"-delete", jsonObject.toString());
	}

}
