package eu.openminted.registry.core.monitor;

import org.springframework.beans.factory.annotation.Autowired;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.jms.JMSService;
import eu.openminted.registry.core.solr.functions.SolrIndexDataFunction;

public class ResourceListenerImpl implements ResourceListener{
	
	@Autowired
	SolrIndexDataFunction solrDataFunction;
	
	@Autowired
	JMSService jmsService;
	
	@Override
	public void resourceAdded(Resource resource) {
		solrDataFunction.add(resource);
		jmsService.publishMessage(resource.getResourceType()+"-create", "Create message");
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		solrDataFunction.update(newResource);
		jmsService.publishMessage(previousResource.getResourceType()+"-update", "Update message");
		
	}

	@Override
	public void resourceDeleted(Resource resource) {
		solrDataFunction.delete(resource);
		jmsService.publishMessage(resource.getResourceType()+"-delete", "Delete message");
	}

}
