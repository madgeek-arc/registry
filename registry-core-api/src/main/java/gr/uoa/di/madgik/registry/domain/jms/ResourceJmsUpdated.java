package gr.uoa.di.madgik.registry.domain.jms;

import gr.uoa.di.madgik.registry.domain.Resource;

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
