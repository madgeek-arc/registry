/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
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

    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(name = "resourcetype_aliases", joinColumns = @JoinColumn(name = "resourcetype_name"))
    @Column(name = "aliases")
    private Set<String> aliases = new HashSet<>();

    @OneToMany(mappedBy = "resourceType", cascade = {CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Resource> resources;

    @OneToMany(mappedBy = "resourceType", cascade = {CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Version> versions;

    @ElementCollection(fetch = FetchType.EAGER)
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
