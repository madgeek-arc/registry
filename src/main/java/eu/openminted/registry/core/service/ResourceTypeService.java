package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.domain.index.IndexField;

import java.util.List;
import java.util.Set;

public interface ResourceTypeService {
	Schema getSchema(String id);

	ResourceType getResourceType(String name);

	List<ResourceType> getAllResourceType();

	List<ResourceType> getAllResourceType(int from, int to);

	ResourceType addResourceType(ResourceType resourceType) throws ServiceException;

	Set<IndexField> getResourceTypeIndexFields(String name);
}
