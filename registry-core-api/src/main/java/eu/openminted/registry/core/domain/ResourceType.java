package eu.openminted.registry.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import eu.openminted.registry.core.domain.index.IndexField;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "ResourceType")
public class ResourceType {

    @Id
    @Size(min = 3, max = 50)
    @Column(name = "name", nullable = false)
    @Access(AccessType.PROPERTY)
    private String name;

    @Column(name = "schema", nullable = false, columnDefinition = "text")
    private String schema;

    @Column
    private String schemaUrl;

    @Size(min = 3, max = 30)
    @Column(name = "payloadType", nullable = false)
    private String payloadType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", nullable = false, updatable = false)
    private Date creationDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modification_date", nullable = false)
    private Date modificationDate;

    @Column
    private String indexMapperClass;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JsonManagedReference(value = "resourcetype-indexfields")
    @Column
    private List<IndexField> indexFields;

    @Column
    private String aliasGroup;

    @OneToMany(mappedBy = "resourceType", cascade = {CascadeType.ALL})
    @JsonIgnore
    @LazyCollection(LazyCollectionOption.TRUE)
    private List<Resource> resources;

    @OneToMany(mappedBy = "resourceType")
    @LazyCollection(LazyCollectionOption.TRUE)
    @JsonIgnore
    private List<Version> versions;

    public ResourceType() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
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

    public String getSchemaUrl() {
        return schemaUrl;
    }

    public void setSchemaUrl(String schemaUrl) {
        this.schemaUrl = schemaUrl;
    }

    public String getIndexMapperClass() {
        return indexMapperClass;
    }

    public void setIndexMapperClass(String indexMapperClass) {
        this.indexMapperClass = indexMapperClass;
    }

    public List<IndexField> getIndexFields() {
        return indexFields;
    }

    public void setIndexFields(List<IndexField> indexFields) {
        this.indexFields = indexFields;
    }

    @PrePersist
    protected void onCreate() {
        modificationDate = creationDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        modificationDate = new Date();
    }


    public String getAliasGroup() {
        return aliasGroup;
    }

    public void setAliasGroup(String aliasGroup) {
        this.aliasGroup = aliasGroup;
    }

    @JsonIgnore
    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    @JsonIgnore
    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }
}
