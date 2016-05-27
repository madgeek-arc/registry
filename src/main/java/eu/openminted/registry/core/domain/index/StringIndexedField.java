package eu.openminted.registry.core.domain.index;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by antleb on 5/24/16.
 */
@Entity
public class StringIndexedField extends IndexedField<String> {

	private String value;

	public StringIndexedField() {
	}

	public StringIndexedField(String name, String value) {
		setName(name);
		setValue(value);
		setType(String.class.getName());
	}

	@Override
	@Column
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}
}
