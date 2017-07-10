package eu.openminted.registry.core.index;

import java.util.Set;

import eu.openminted.registry.core.domain.index.BooleanIndexedField;
import org.springframework.stereotype.Repository;

import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.domain.index.StringIndexedField;

/**
 * Created by antleb on 5/24/16.
 */
@Repository("indexedFieldFactory")
public class IndexedFieldFactory {


	public <T> IndexedField<T> getIndexedField(String fieldName, Set<Object> value, String fieldType) {

		// Set<String> set = value.stream().map(Object::toString).collect(Collectors.toSet());
		IndexedField field = null;
		if (String.class.getName().equals(fieldType)) {
			field = new StringIndexedField(fieldName,value);
		} else if (Boolean.class.getName().equals(fieldType)) {
			field = new BooleanIndexedField(fieldName,value);
		}
		return field;
	}
}
