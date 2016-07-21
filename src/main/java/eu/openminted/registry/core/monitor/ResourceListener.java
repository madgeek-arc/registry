package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.domain.Resource;

/**
 * Created by antleb on 5/26/16.
 */
public interface ResourceListener {

	public void resourceAdded(Resource resource);

	public void resourceUpdated(Resource previousResource, Resource newResource);

	public void resourceDeleted(Resource resource);
}
