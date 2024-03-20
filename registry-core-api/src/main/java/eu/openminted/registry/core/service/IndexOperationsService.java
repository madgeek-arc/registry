package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface IndexOperationsService {

    void add(Resource resource);

    void addBulk(List<Resource> resources);

    void update(Resource previousResource, Resource newResource);

    void delete(String resourceId, String resourceType);

    void delete(Resource resource);

    void createIndex(ResourceType resourceType);

    void deleteIndex(String name);
}
