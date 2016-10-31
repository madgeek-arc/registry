package eu.openminted.registry.core.elasticsearch.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.monitor.ResourceListener;

@Component
public class ElasticResourceListener implements ResourceListener {

	@Autowired
	ElasticOperationsService elasticOperationsService;
	
	@Override
	public void resourceAdded(Resource resource) {
		// TODO Auto-generated method stub
		elasticOperationsService.add(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		// TODO Auto-generated method stub
		elasticOperationsService.update(previousResource, newResource);
	}

	@Override
	public void resourceDeleted(Resource resource) {
		// TODO Auto-generated method stub
		elasticOperationsService.delete(resource);
	}

}
