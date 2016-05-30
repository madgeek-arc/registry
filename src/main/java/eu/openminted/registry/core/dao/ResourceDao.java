package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;

import java.util.List;

public interface ResourceDao {

	Resource getResource(String resourceType, String id);
	
	public List<Resource> getResource(String resourceType);
	
	public List<Resource> getResource(String resourceType, int from, int to);
	
	List<Resource> getResource(int from, int to);
	
	public List<Resource> getResource();
	
	void addResource(Resource resource);
	
	void updateResource(Resource resource);
	
	void deleteResource(String id);
	
}
