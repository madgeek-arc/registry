package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceType;

public interface ViewDao {

    void createView(ResourceType resourceType);

    void deleteView(String resourceType);

}
