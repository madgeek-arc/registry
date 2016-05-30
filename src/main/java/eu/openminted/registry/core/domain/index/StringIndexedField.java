package eu.openminted.registry.core.domain.index;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Created by antleb on 5/24/16.
 */

@Entity
public class StringIndexedField extends IndexedField<String> {
	
	@Column
	@ElementCollection
	private Set<String> values;

	public StringIndexedField() {
	}

	public StringIndexedField(String name, Set<String> values) {
		setName(name);
		setValues(values);
		setType(String.class.getName());
	}

	@Override
	public Set<String> getValues() {
		return values;
	}

	@Override
	public void setValues(Set<String> value) {
		this.values = value;
	}
}
