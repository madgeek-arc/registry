package eu.openminted.registry.core.elasticsearch.listeners;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticResourceTypeListener implements ResourceTypeListener {

	private Logger logger = LoggerFactory.getLogger(ElasticResourceTypeListener.class);
	
	@Autowired
	ElasticOperationsService elasticOperationsService;
	
	@Override
	public void resourceTypeAdded(ResourceType resourceType) {
		elasticOperationsService.createIndex(resourceType);
	}

	@Override
	public void resourceTypeDelete(String name) {
		elasticOperationsService.deleteIndex(name);
	}

}
