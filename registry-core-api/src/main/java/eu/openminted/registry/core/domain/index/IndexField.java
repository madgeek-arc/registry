package eu.openminted.registry.core.domain.index;

import com.fasterxml.jackson.annotation.JsonBackReference;
import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
public class IndexField implements Serializable{

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
		return (StringUtils.isEmpty(label) ? name : label);
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		IndexField that = (IndexField) o;

		if (multivalued != that.multivalued) return false;
		if (primaryKey != that.primaryKey) return false;
		if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (path != null ? !path.equals(that.path) : that.path != null) return false;
		if (type != null ? !type.equals(that.type) : that.type != null) return false;
		if (label != null ? !label.equals(that.label) : that.label != null) return false;
		return defaultValue != null ? defaultValue.equals(that.defaultValue) : that.defaultValue == null;

	}

	@Override
	public int hashCode() {
		int result = resourceType != null ? resourceType.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (path != null ? path.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (label != null ? label.hashCode() : 0);
		result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
		result = 31 * result + (multivalued ? 1 : 0);
		result = 31 * result + (primaryKey ? 1 : 0);
		return result;
	}
}
