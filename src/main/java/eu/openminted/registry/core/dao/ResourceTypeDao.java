package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;

import java.util.List;

public interface ResourceTypeDao {

	ResourceType getResourceType(String name);
	
	List<ResourceType> getAllResourceType();
	
	List<ResourceType> getAllResourceType(int from, int to);
	
	void addResourceType(ResourceType resource);
	
}
