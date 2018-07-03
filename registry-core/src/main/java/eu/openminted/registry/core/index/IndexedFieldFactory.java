package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.index.*;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Set;

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
		} else if (Integer.class.getName().equals(fieldType)){
			field = new IntegerIndexedField(fieldName,value);
		} else if (Date.class.getName().equals(fieldType)) {
			field = new DateIndexedField(fieldName, value);
		}
		return field;
	}
}
