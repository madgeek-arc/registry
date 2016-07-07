package eu.openminted.registry.core.monitor;

import org.springframework.beans.factory.annotation.Autowired;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.jms.JMSFunctions;
import eu.openminted.registry.core.solr.functions.SolrIndexDataFunction;

public class ResourceListenerImpl implements ResourceListener{
	
	@Autowired
	SolrIndexDataFunction solrDataFunction;
	
	@Override
	public void resourceAdded(Resource resource) {
		solrDataFunction.add(resource);
		JMSFunctions jmsFunctions = new JMSFunctions();
		jmsFunctions.publishMessage(resource.getResourceType()+"-create", "Create message");
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		solrDataFunction.update(newResource);
		JMSFunctions jmsFunctions = new JMSFunctions();
		jmsFunctions.publishMessage(previousResource.getResourceType()+"-update", "Update message");
		
	}

	@Override
	public void resourceDeleted(Resource resource) {
		solrDataFunction.delete(resource);
		JMSFunctions jmsFunctions = new JMSFunctions();
		jmsFunctions.publishMessage(resource.getResourceType()+"-delete", "Delete message");
	}

}
