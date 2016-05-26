package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.domain.index.StringIndexedField;
import org.springframework.stereotype.Repository;

/**
 * Created by antleb on 5/24/16.
 */
@Repository("indexedFieldFactory")
public class IndexedFieldFactory {


	public <T> IndexedField<T> getIndexedField(String fieldName, Object value, String fieldType) {

		if (String.class.getName().equals(fieldType)) {
			return (IndexedField<T>) new StringIndexedField(fieldName, (String) value);
		}

		return null;
	}
}
