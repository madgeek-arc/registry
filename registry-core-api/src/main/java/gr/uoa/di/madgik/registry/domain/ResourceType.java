/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "ResourceType")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "creation_date", nullable = false, updatable = false)
    private Instant creationDate;

    @Column(name = "modification_date", nullable = false)
    private Instant modificationDate;

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "modified_by", nullable = false, length = 255)
    private String modifiedBy;

    @Column
    private String indexMapperClass;

    // Without an explicit ORDER BY, Hibernate gives no guaranteed iteration order for this Set,
    // and it can vary between fetches of the same data (query plan, cache hit/miss, etc.). This
    // doesn't affect correctness anywhere — every consumer (search matching, KeyValue arrays,
    // resolvePrimaryKeyValues) is field-name-keyed and order-independent by construction. It only
    // affects the readability/reproducibility of composite-key log lines and exception messages
    // that join multiple primaryKey fields into "field=value,field=value" strings (see
    // GenericResourceManager.extractPrimaryKeys/resolvePrimaryKeyValues): without this, the same
    // resource type's fields could print in a different order between two otherwise-identical
    // calls. @OrderBy makes that output deterministic.
    @OneToMany(mappedBy = "resourceType", fetch = FetchType.EAGER, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderBy("name ASC")
    @JsonManagedReference(value = "resourcetype-indexfields")
    private Set<IndexField> indexFields = new LinkedHashSet<>();

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
        // no-arg constructor
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

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Instant getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Instant modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
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
        if (indexFields == null) {
            return null;
        }
        return new ArrayList<>(indexFields);
    }

    public void setIndexFields(List<IndexField> indexFields) {
        if (indexFields == null) {
            if (this.indexFields == null) {
                this.indexFields = null;
            } else {
                this.indexFields.clear();
            }
            return;
        }
        if (this.indexFields == null) {
            this.indexFields = new LinkedHashSet<>();
        } else {
            this.indexFields.clear();
        }
        this.indexFields.addAll(indexFields);
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
        modificationDate = creationDate = Instant.now();
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = "system";
        }
        if (modifiedBy == null || modifiedBy.isBlank()) {
            modifiedBy = createdBy;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modificationDate = Instant.now();
        if (modifiedBy == null || modifiedBy.isBlank()) {
            modifiedBy = createdBy == null || createdBy.isBlank() ? "system" : createdBy;
        }
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    @JsonIgnore
    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

}
