package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;

/**
 * Created by antleb on 5/26/16.
 */
public interface ResourceListener {

	void resourceAdded(Resource resource);

	void resourceUpdated(Resource previousResource, Resource newResource);

	void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType);

	void resourceDeleted(Resource resource);
}
