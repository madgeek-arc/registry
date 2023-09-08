package eu.openminted.registry.core.elasticsearch.listeners;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.monitor.ResourceListener;
import org.springframework.stereotype.Component;

@Component
public class ElasticResourceListener implements ResourceListener {

	private final ElasticOperationsService elasticOperationsService;

	public ElasticResourceListener(ElasticOperationsService elasticOperationsService) {
		this.elasticOperationsService = elasticOperationsService;
	}
	
	@Override
	public void resourceAdded(Resource resource) {
		elasticOperationsService.add(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		elasticOperationsService.update(previousResource, newResource);
	}

	@Override
	public void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType) {
		elasticOperationsService.delete(resource.getId(), previousResourceType.getName());
		elasticOperationsService.add(resource);
	}

	@Override
	public void resourceDeleted(Resource resource) {
		elasticOperationsService.delete(resource);
	}

}
