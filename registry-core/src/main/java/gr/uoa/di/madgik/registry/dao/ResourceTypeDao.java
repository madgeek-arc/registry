package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.List;
import java.util.Set;

public interface ResourceTypeDao {

    ResourceType getResourceType(String name);

    List<ResourceType> getAllResourceType();

    List<ResourceType> getAllResourceType(int from, int to);

    void addResourceType(ResourceType resource);

    Set<IndexField> getResourceTypeIndexFields(String name);

    void deleteResourceType(String resourceType);

}
