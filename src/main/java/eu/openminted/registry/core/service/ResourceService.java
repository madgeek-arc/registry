package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Resource;

import java.util.List;

public interface ResourceService {
	Resource getResource(String resourceType, String id);

	List<Resource> getResource(String resourceType);

	List<Resource> getResource(String resourceType, int from, int to);

	List<Resource> getResource(int from, int to);

	List<Resource> getResource();

	Resource addResource(Resource resource) throws ServiceException;

	Resource updateResource(Resource resource) throws ServiceException;

	void deleteResource(String id);
}

