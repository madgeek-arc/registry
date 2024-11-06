package gr.uoa.di.madgik.registry.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Resource")
public class Resource {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_name", nullable = false)
    @JsonBackReference(value = "resourcetype-resource")
    private ResourceType resourceType;

    @Transient
    private String resourceTypeName;

    @Size(min = 3, max = 50)
    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Transient
    @JsonIgnore
    private String payloadUrl;

    @Transient
    @JsonIgnore
    private String searchableArea;

    @Size(min = 3, max = 30)
    @Column(name = "payloadFormat", nullable = false)
    private String payloadFormat;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", nullable = false, updatable = false)
    private Date creationDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modification_date", nullable = false)
    private Date modificationDate;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true, mappedBy = "resource", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<IndexedField> indexedFields;

    @OneToMany(mappedBy = "resource", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Version> versions;

    public Resource(String id, ResourceType resourceType, String version, String payload, String payloadFormat) {
        this.id = id;
        this.resourceType = resourceType;
        this.version = version;
        this.payload = payload;
        this.payloadFormat = payloadFormat;
    }

    public Resource() {

    }

    public String getId() {
        return id;
    }

    public void setId(String string) {
        this.id = string;
    }

    //    @JsonIgnore
    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

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

    public String getPayloadFormat() {
        return payloadFormat;
    }

    public void setPayloadFormat(String payloadFormat) {
        this.payloadFormat = payloadFormat;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getPayloadUrl() {
        return payloadUrl;
    }

    public void setPayloadUrl(String payloadUrl) {
        this.payloadUrl = payloadUrl;
    }

    public List<IndexedField> getIndexedFields() {
        return indexedFields;
    }

    public void setIndexedFields(List<IndexedField> indexedFields) {
        this.indexedFields = indexedFields;
    }

    public String getSearchableArea() {
        return searchableArea;
    }

    public void setSearchableArea(String searchableArea) {
        this.searchableArea = searchableArea;
    }

    @PrePersist
    protected void onCreate() {

        if (creationDate == null)
            creationDate = new Date();

        if (modificationDate == null)
            modificationDate = new Date();

        version = generateVersion();
    }

    @PreUpdate
    protected void onUpdate() {

        modificationDate = new Date();
        version = generateVersion();
    }

    @PreRemove
    public void removeReferenceOfChildren() {
        if (versions != null) {
            for (Version v : versions) {
                v.setResource(null);
            }
        }
    }

    @JsonIgnore
    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    private String generateVersion() {
        DateFormat df = new SimpleDateFormat("MMddyyyyHHmmss");
        return df.format(Calendar.getInstance().getTime());
    }

    public String getResourceTypeName() {
        return (resourceType == null) ? resourceTypeName : resourceType.getName();
    }

    public void setResourceTypeName(String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
    }

}
