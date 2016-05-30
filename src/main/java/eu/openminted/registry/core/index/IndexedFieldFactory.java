package eu.openminted.registry.core.index;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.domain.index.StringIndexedField;

/**
 * Created by antleb on 5/24/16.
 */
@Repository("indexedFieldFactory")
public class IndexedFieldFactory {


	public <T> IndexedField<T> getIndexedField(String fieldName, Set<Object> value, String fieldType) {

		Set<String> set = new HashSet<String>();
		for(Object singleValue:value) {
			set.add((String)singleValue);
		}

		if (String.class.getName().equals(fieldType)) {
			return (IndexedField<T>) new StringIndexedField(fieldName, set);
		}

		return null;
	}
}
