package eu.openminted.registry.core.domain.index;

import com.fasterxml.jackson.annotation.JsonBackReference;
import eu.openminted.registry.core.domain.Resource;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class IndexedField<T extends Object> implements Serializable {

	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	@JsonBackReference
	private Resource resource;
	@Id
	@Column
	private String name;
	@Column
	private String type;

	public IndexedField() {
	}

	public abstract T getValue();

	public abstract void setValue(T value);

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}
}
