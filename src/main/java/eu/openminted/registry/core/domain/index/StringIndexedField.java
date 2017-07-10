package eu.openminted.registry.core.domain.index;

import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;


/**
 * Created by antleb on 5/24/16.
 */
@Entity
public class StringIndexedField extends IndexedField<String> {

	public StringIndexedField() {
	}

	public StringIndexedField(String name, Set<Object> values) {
		setName(name);
		setValues(values.stream().map(Object::toString).collect(Collectors.toSet()));
	}

}
