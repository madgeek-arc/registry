package eu.openminted.registry.core.dao;

import java.util.List;

import eu.openminted.registry.core.domain.ResourceType;

public interface ResourceTypeDao {

	ResourceType getResourceType(String name);
	
	List<ResourceType> getAllResourceType();
	
	List<ResourceType> getAllResourceType(int from, int to);
	
	void addResourceType(ResourceType resource);
	
}
