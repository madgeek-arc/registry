package eu.openminted.registry.core.elasticsearch.listeners;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import eu.openminted.registry.core.service.IndexOperationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
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
