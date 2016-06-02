package eu.openminted.registry.core.domain.index;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonManagedReference;


@Entity
public class FloatIndexedField extends IndexedField<Float> {
	
	@Column
	@ElementCollection
	private Set<Float> values;
	
	public FloatIndexedField() {
	}

	public FloatIndexedField(String name, Set<Float> values) {
		setName(name);
		setValues(values);
		setType(String.class.getName());
	}

	@Override
	public Set<Float> getValues() {
		Hibernate.initialize(values);
		return values;
	}

	@Override
	public void setValues(Set<Float> value) {
		this.values = value;
	}
}
