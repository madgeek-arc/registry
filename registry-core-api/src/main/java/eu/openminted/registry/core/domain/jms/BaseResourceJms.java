package eu.openminted.registry.core.domain.jms;

import eu.openminted.registry.core.domain.Resource;

public class BaseResourceJms {

    private String resourceId;

    private String resourceType;

    private String resource;

    private String payloadFormat;

    public BaseResourceJms() {
    }

    public BaseResourceJms(Resource resource) {
        this.resourceId = resource.getId();
        this.resource = resource.getPayload();
        this.resourceType = resource.getResourceType().getName();
        this.payloadFormat = resource.getPayloadFormat();
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getPayloadFormat() {
        return payloadFormat;
    }

    public void setPayloadFormat(String payloadFormat) {
        this.payloadFormat = payloadFormat;
    }
}
