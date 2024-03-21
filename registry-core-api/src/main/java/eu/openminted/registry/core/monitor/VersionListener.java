package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.domain.Resource;

/**
 * Created by antleb on 5/26/16.
 */
public interface VersionListener {

    void versionAdded(Resource resource);

    void versionUpdated(Resource previousResource, Resource newResource);

}
