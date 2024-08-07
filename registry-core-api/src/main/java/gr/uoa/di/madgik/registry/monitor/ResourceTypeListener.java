package gr.uoa.di.madgik.registry.monitor;

import gr.uoa.di.madgik.registry.domain.ResourceType;

/**
 * Created by antleb on 5/30/16.
 */
public interface ResourceTypeListener {

    public void resourceTypeAdded(ResourceType resourceType);

    public void resourceTypeDelete(String name);
}
