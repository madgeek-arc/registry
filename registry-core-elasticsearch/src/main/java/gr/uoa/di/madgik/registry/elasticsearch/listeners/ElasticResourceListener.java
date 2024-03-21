package gr.uoa.di.madgik.registry.elasticsearch.listeners;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
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
