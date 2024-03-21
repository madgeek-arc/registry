package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.domain.ResourceType;

/**
 * Created by antleb on 5/30/16.
 */
public interface ResourceTypeListener {

    public void resourceTypeAdded(ResourceType resourceType);

    public void resourceTypeDelete(String name);
}
