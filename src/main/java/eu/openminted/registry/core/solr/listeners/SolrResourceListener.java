package eu.openminted.registry.core.solr.listeners;

import org.springframework.beans.factory.annotation.Autowired;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import eu.openminted.registry.core.solr.functions.SolrIndexDataFunction;

public class SolrResourceListener implements ResourceListener{
	@Autowired
	SolrIndexDataFunction solrDataFunction;
	
	@Override
	public void resourceAdded(Resource resource) {
		solrDataFunction.add(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		solrDataFunction.update(newResource);
	}

	@Override
	public void resourceDeleted(Resource resource) {
		solrDataFunction.delete(resource);
	}

}
