package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;

public interface ViewDao {

	void createView(ResourceType resourceType);

	void deleteView(String resourceType);

}
