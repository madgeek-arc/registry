package eu.openminted.registry.core.domain.index;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by antleb on 5/24/16.
 */
@Entity
@Table
public class StringIndexedField extends IndexedField<String> {


	@Column
	@ElementCollection
	@LazyCollection(LazyCollectionOption.FALSE)
	private Set<String> values;

	public StringIndexedField() {
	}

	@Override
	public Set<String> getValues() {
		return values;
	}

	@Override
	public void setValues(Set<String> value) {
		this.values = value;
	}

	public StringIndexedField(String name, Set<Object> values) {
		setName(name);
		setValues(values.stream().map(Object::toString).collect(Collectors.toSet()));
	}

}
