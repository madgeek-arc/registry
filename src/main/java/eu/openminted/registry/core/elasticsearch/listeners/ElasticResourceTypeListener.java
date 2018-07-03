package eu.openminted.registry.core.elasticsearch.listeners;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.monitor.ResourceTypeListener;

@Component
public class ElasticResourceTypeListener implements ResourceTypeListener {

	private Logger logger = LogManager.getLogger(ElasticResourceTypeListener.class);
	
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
