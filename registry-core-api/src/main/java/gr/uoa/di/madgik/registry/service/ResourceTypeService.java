package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.List;
import java.util.Set;

public interface ResourceTypeService {
    Schema getSchema(String id);

    ResourceType getResourceType(String name);

    List<ResourceType> getAllResourceType();

    List<ResourceType> getAllResourceType(int from, int to);

    ResourceType addResourceType(ResourceType resourceType) throws ServiceException;

    Set<IndexField> getResourceTypeIndexFields(String name);

    void deleteResourceType(String name);
}
