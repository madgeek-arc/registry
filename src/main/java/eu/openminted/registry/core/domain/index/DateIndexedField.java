package eu.openminted.registry.core.domain.index;

import java.util.Date;
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
public class DateIndexedField extends IndexedField<Date> {
	
	@Column
	@ElementCollection
	private Set<Date> values;
	
	public DateIndexedField() {
	}

	public DateIndexedField(String name, Set<Date> values) {
		setName(name);
		setValues(values);
		setType(String.class.getName());
	}

	@Override
	public Set<Date> getValues() {
		Hibernate.initialize(values);
		return values;
	}

	@Override
	public void setValues(Set<Date> value) {
		this.values = value;
	}
}
