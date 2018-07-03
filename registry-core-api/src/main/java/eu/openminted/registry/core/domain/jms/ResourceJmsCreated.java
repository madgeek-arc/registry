package eu.openminted.registry.core.domain.jms;

import eu.openminted.registry.core.domain.Resource;

public class ResourceJmsCreated extends BaseResourceJms {

    public ResourceJmsCreated() {

    }

    public ResourceJmsCreated(Resource resource) {
        super(resource);
    }
}
