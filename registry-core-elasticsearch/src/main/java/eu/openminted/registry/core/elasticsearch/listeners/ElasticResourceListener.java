package eu.openminted.registry.core.elasticsearch.listeners;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.IndexOperationsService;
import eu.openminted.registry.core.monitor.ResourceListener;
import org.springframework.stereotype.Component;

@Component
public class ElasticResourceListener implements ResourceListener {

	private final IndexOperationsService indexOperationsService;

	public ElasticResourceListener(IndexOperationsService indexOperationsService) {
		this.indexOperationsService = indexOperationsService;
	}
	
	@Override
	public void resourceAdded(Resource resource) {
		indexOperationsService.add(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		indexOperationsService.update(previousResource, newResource);
	}

	@Override
	public void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType) {
		indexOperationsService.delete(resource.getId(), previousResourceType.getName());
		indexOperationsService.add(resource);
	}

	@Override
	public void resourceDeleted(Resource resource) {
		indexOperationsService.delete(resource);
	}

}
