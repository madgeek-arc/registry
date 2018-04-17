package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.ResourceType;

public interface ViewService {

	void createView(ResourceType resourceType);

	void deleteView(String resourceType);

}
