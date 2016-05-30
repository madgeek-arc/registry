package eu.openminted.registry.core.domain.index;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Set;

/**
 * Created by antleb on 5/24/16.
 */
@Entity
public class StringIndexedField extends IndexedField<String> {

	private Set<String> values;

	public StringIndexedField() {
	}

	public StringIndexedField(String name, Set<String> values) {
		setName(name);
		setValues(values);
		setType(String.class.getName());
	}

	@Override
	@Column
	public Set<String> getValues() {
		return values;
	}

	@Override
	public void setValues(Set<String> value) {
		this.values = value;
	}
}
