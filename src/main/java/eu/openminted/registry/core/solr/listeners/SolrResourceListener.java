package eu.openminted.registry.core.solr.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import eu.openminted.registry.core.solr.service.SolrOperationsService;

@Component
public class SolrResourceListener implements ResourceListener{
	
	@Autowired
	SolrOperationsService solrOperationsService;
	
	@Override
	public void resourceAdded(Resource resource) {
//		solrOperationsService.add(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
//		solrOperationsService.update(newResource);
	}

	@Override
	public void resourceDeleted(Resource resource) {
//		solrOperationsService.delete(resource);
	}

}
