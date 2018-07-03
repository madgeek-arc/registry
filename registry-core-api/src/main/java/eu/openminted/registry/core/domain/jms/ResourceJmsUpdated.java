package eu.openminted.registry.core.domain.jms;

import eu.openminted.registry.core.domain.Resource;

public class ResourceJmsUpdated extends BaseResourceJms {

    private String previous;

    public ResourceJmsUpdated() {

    }

    public ResourceJmsUpdated(Resource resource, Resource oldResource) {
        super(resource);
        previous = oldResource.getPayload();
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }
}
