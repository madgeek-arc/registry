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
public class IntegerIndexedField extends IndexedField<Integer> {
	
	@Column
	@ElementCollection
	private Set<Integer> values;
	
	public IntegerIndexedField() {
	}

	public IntegerIndexedField(String name, Set<Integer> values) {
		setName(name);
		setValues(values);
		setType(String.class.getName());
	}

	@Override
	public Set<Integer> getValues() {
		Hibernate.initialize(values);
		return values;
	}

	@Override
	public void setValues(Set<Integer> value) {
		this.values = value;
	}
}
