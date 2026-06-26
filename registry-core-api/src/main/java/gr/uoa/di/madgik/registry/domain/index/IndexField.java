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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
public class IndexField implements Serializable {

    private static final float DEFAULT_NULL_EMBEDDING_WEIGHT = 1.0f;

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

    @Comment("Human-readable display label. Used for facet headings and as the semantic prefix " +
            "in chunk embeddings. A field must have both a non-null label and KEYWORD capability " +
            "to be eligible for the auto-derived browseBy/facet set.")
    @Column
    private String label;

    @Column
    private String defaultValue;

    @Column
    private boolean multivalued;

    @Column
    private boolean primaryKey = false;

    @Comment("Search capabilities for string fields (KEYWORD, TEXT, or both). Defaults to KEYWORD " +
            "when unset. KEYWORD gates facet/browseBy eligibility and SQL keyword highlights; " +
            "TEXT adds a .text analyzed sub-field in Elasticsearch and enables sentence-level chunking.")
    @Convert(converter = SearchCapabilitySetConverter.class)
    @Column(name = "search_capabilities")
    private Set<SearchCapability> searchCapabilities = EnumSet.noneOf(SearchCapability.class);

    @Comment("The weight this index field will have when creating an embedding vector for the resource.")
    @Column(name = "embedding_weight", columnDefinition = "real")
    @Check(constraints = "embedding_weight >= 0")
    private Float embeddingWeight;

    @Comment("The name of the ResourceType whose resource IDs appear as values for this field. " +
            "When set, FacetLabelService will resolve Value.label for facets backed by this field.")
    @Column(name = "related_resource_type")
    private String relatedResourceType;

    @Comment("The IndexField name in the relatedResourceType to use as the display label. " +
            "Falls back to a field named name in the related type if null.")
    @Column(name = "related_resource_type_field")
    private String relatedResourceTypeField;

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

    public Set<SearchCapability> getSearchCapabilities() {
        if (searchCapabilities == null || searchCapabilities.isEmpty()) {
            return EnumSet.of(SearchCapability.KEYWORD);
        }
        return EnumSet.copyOf(searchCapabilities);
    }

    public void setSearchCapabilities(Set<SearchCapability> searchCapabilities) {
        if (searchCapabilities == null || searchCapabilities.isEmpty()) {
            this.searchCapabilities = EnumSet.noneOf(SearchCapability.class);
            return;
        }
        this.searchCapabilities = EnumSet.copyOf(searchCapabilities);
    }

    public boolean hasSearchCapability(SearchCapability capability) {
        return getSearchCapabilities().contains(capability);
    }

    public float getEmbeddingWeight() {
        if (embeddingWeight == null) {
            return "java.lang.String".equals(type) ? DEFAULT_NULL_EMBEDDING_WEIGHT : 0.0f;
        }
        return embeddingWeight;
    }

    public void setEmbeddingWeight(Float embeddingWeight) {
        this.embeddingWeight = embeddingWeight;
    }

    public String getRelatedResourceType() {
        return relatedResourceType;
    }

    public void setRelatedResourceType(String relatedResourceType) {
        this.relatedResourceType = relatedResourceType;
    }

    public String getRelatedResourceTypeField() {
        return relatedResourceTypeField;
    }

    public void setRelatedResourceTypeField(String relatedResourceTypeField) {
        this.relatedResourceTypeField = relatedResourceTypeField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexField that)) {
            return false;
        }
        return Objects.equals(resourceTypeName(resourceType), resourceTypeName(that.resourceType))
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceTypeName(resourceType), name);
    }

    private static String resourceTypeName(ResourceType resourceType) {
        return resourceType == null ? null : resourceType.getName();
    }
}
