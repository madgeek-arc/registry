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

package gr.uoa.di.madgik.registry.domain.index;

import com.fasterxml.jackson.annotation.JsonBackReference;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Comment;

import java.io.Serializable;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
public class IndexField implements Serializable {

    @ManyToOne(fetch = FetchType.EAGER)
    @Id
    @JsonBackReference(value = "resourcetype-indexfields")
    private ResourceType resourceType;

    @Column
    @Id
    private String name;

    @Column
    private String path;

    @Column
    private String type;

    @Column
    private String label;

    @Column
    private String defaultValue;

    @Column
    private boolean multivalued;

    @Column
    private boolean primaryKey = false;

    @Comment("The weight this index field will have when creating an embedding vector for the resource.")
    @Column(name = "embedding_weight")
    @Check(constraints = "embedding_weight >= 0")
    private Float embeddingWeight = 0.0f;

    public IndexField() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    public void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public float getEmbeddingWeight() {
        if (embeddingWeight == null) {
            return 0.0f;
        }
        return embeddingWeight;
    }

    public void setEmbeddingWeight(float embeddingWeight) {
        this.embeddingWeight = embeddingWeight;
    }
}
