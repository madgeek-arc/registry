package gr.uoa.di.madgik.registry.domain.index;

import com.fasterxml.jackson.annotation.JsonBackReference;
import gr.uoa.di.madgik.registry.domain.ResourceType;

import jakarta.persistence.*;
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

}
