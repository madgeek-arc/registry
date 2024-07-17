package gr.uoa.di.madgik.registry.monitor;

import gr.uoa.di.madgik.registry.domain.Resource;

/**
 * Created by antleb on 5/26/16.
 */
public interface VersionListener {

    void versionAdded(Resource resource);

    void versionUpdated(Resource previousResource, Resource newResource);

}
