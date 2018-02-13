package eu.openminted.registry.core.domain.index;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.openminted.registry.core.domain.Resource;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class IndexedField<T extends Object> implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JsonBackReference
	@JsonIgnore
	private Resource resource;

	@Column
	private String name;

	@Column
	@ElementCollection
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<String> values;

	public IndexedField() {
	}

	public IndexedField(String name, Set<String> values) {
		setName(name);
		setValues(values);
	}

	public Set<String> getValues() {
		return values;
	}

	public void setValues(Set<String> value) {
		this.values = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		IndexedField<?> that = (IndexedField<?>) o;

		if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		return values != null ? values.equals(that.values) : that.values == null;

	}

	@Override
	public int hashCode() {
		int result = resource != null ? resource.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (values != null ? values.hashCode() : 0);
		return result;
	}
}
