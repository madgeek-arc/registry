package gr.uoa.di.madgik.registry.monitor;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;

/**
 * Created by antleb on 5/26/16.
 */
public interface ResourceListener {

    void resourceAdded(Resource resource);

    void resourceUpdated(Resource previousResource, Resource newResource);

    void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType);

    void resourceDeleted(Resource resource);
}
