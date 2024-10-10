package gr.uoa.di.madgik.registry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Entity
@Table(name = "ResourceType")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ResourceType implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JsonManagedReference(value = "resourcetype-indexfields")
    @Column
    private List<IndexField> indexFields;

    /**
     * @deprecated This field has been replaced with the multivalued 'aliases' field.
     */
    @Deprecated
    @Column
    private String aliasGroup;

    @ElementCollection
//    @CollectionTable(name = "resourcetype_aliases", joinColumns = @JoinColumn(name = "resourcetype_name"))
    @Column(name = "aliases")
    private Set<String> aliases = new HashSet<>();

    @OneToMany(mappedBy = "resourceType", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JsonIgnore
    @LazyCollection(LazyCollectionOption.TRUE)
    private List<Resource> resources;

    @OneToMany(mappedBy = "resourceType", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @LazyCollection(LazyCollectionOption.TRUE)
    @JsonIgnore
    private List<Version> versions;

    @ElementCollection
    private Map<String, String> properties = new HashMap<>();

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

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getProperty(String name) {
        return this.properties.get(name);
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

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliasGroups) {
        this.aliases = aliasGroups;
    }

    @JsonIgnore
    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

}
