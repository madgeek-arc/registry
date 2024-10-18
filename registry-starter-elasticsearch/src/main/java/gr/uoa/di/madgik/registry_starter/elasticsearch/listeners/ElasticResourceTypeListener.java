package gr.uoa.di.madgik.registry_starter.elasticsearch.listeners;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.monitor.ResourceTypeListener;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticResourceTypeListener implements ResourceTypeListener {

    private final IndexOperationsService indexOperationsService;
    private Logger logger = LoggerFactory.getLogger(ElasticResourceTypeListener.class);

    public ElasticResourceTypeListener(IndexOperationsService indexOperationsService) {
        this.indexOperationsService = indexOperationsService;
    }

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        indexOperationsService.createIndex(resourceType);
    }

    @Override
    public void resourceTypeDelete(String name) {
        indexOperationsService.deleteIndex(name);
    }

}
