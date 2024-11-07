package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public interface ResourceDao {

    Resource getResource(String id);

    List<Resource> getModifiedSince(Date date, String resourceType);

    List<Resource> getModifiedSince(Date date);

    List<Resource> getCreatedSince(Date date);

    List<Resource> getCreatedSince(Date date, String resourceType);

    List<Resource> getResource(ResourceType resourceType);

    Stream<Resource> getResourceStream();

    List<Resource> getResource(ResourceType resourceType, int from, int to);

    List<Resource> getResource(int from, int to);

    List<Resource> getResource();

    Resource addResource(Resource resource);

    Resource updateResource(Resource resource);

    void deleteResource(Resource id);

}
