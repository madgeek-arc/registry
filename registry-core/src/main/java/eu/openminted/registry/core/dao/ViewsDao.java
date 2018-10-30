package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;

public interface ViewsDao {

    void createView(ResourceType resourceType);

    void deleteView(String resourceType);

}
