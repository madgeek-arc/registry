package gr.uoa.di.madgik.registry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Entity
@Table(name = "ResourceVersion")
public class Version {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reference_id")
    private Resource resource;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "resourceType_name")
    private String resourceTypeName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_name_version")
    @JsonIgnore
    private ResourceType resourceType;

    @Size(min = 3, max = 50)
    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", nullable = false, updatable = false)
    private Date creationDate;

    public Version(String id, Resource resource, ResourceType resourceType, String version, String payload, Date creationDate) {
        this.id = id;
        this.resource = resource;
        this.resourceType = resourceType;
        this.version = version;
        this.payload = payload;
    }

    public Version() {

    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    public void setResourceTypeName(String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
    }

    public String getId() {
        return id;
    }

    public void setId(String string) {
        this.id = string;
    }

    @JsonIgnore
    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    @JsonIgnore
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @PrePersist
    protected void onCreate() {
        if (creationDate == null)
            creationDate = new Date();
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
