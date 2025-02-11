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
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

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
