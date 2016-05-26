package eu.openminted.registry.core.domain.index;

import com.fasterxml.jackson.annotation.JsonBackReference;
import eu.openminted.registry.core.domain.ResourceType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class IndexField implements Serializable{

	@ManyToOne(fetch = FetchType.EAGER)
	@Id
	@JsonBackReference
	private ResourceType resourceType;

	@Column
	@Id
	private String name;
	@Column
	private String path;
	@Column
	private String type;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		IndexField that = (IndexField) o;

		if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (path != null ? !path.equals(that.path) : that.path != null) return false;
		return type != null ? type.equals(that.type) : that.type == null;

	}

	@Override
	public int hashCode() {
		int result = resourceType != null ? resourceType.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (path != null ? path.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}
}
