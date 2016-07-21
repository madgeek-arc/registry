package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Schema;

import java.util.List;

public interface ResourceTypeService {
	Schema getSchema(String id);

	ResourceType getResourceType(String name);

	List<ResourceType> getAllResourceType();

	List<ResourceType> getAllResourceType(int from, int to);

	ResourceType addResourceType(ResourceType resourceType) throws ServiceException;
}
