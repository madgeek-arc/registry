package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;

import java.util.List;

public interface ResourceDao {

    Resource getResource(String resourceType, String id);

    List<Resource> getResource(ResourceType resourceType);

    List<Resource> getResource(ResourceType resourceType, int from, int to);

    List<Resource> getResource(int from, int to);

    List<Resource> getResource();

    void addResource(Resource resource);

    void updateResource(Resource resource);

    void deleteResource(String id);

}
