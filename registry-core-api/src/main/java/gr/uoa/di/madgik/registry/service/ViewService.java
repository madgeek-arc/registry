package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.ResourceType;

public interface ViewService {

    void createView(ResourceType resourceType);

    void deleteView(String resourceType);

}

