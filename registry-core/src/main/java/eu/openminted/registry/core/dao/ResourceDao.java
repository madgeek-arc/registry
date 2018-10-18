package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public interface ResourceDao {

    Resource getResource(String id);

    List<Resource> getModifiedSince(Date date);

    List<Resource> getResource(ResourceType resourceType);

    Stream<Resource> getResourceStream();

    List<Resource> getResource(ResourceType resourceType, int from, int to);

    List<Resource> getResource(int from, int to);

    List<Resource> getResource();

    void addResource(Resource resource);

    void updateResource(Resource resource);

    void deleteResource(Resource id);

}
