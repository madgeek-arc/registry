package eu.openminted.registry.core.index;

import java.util.Set;

/**
 * Created by antleb on 5/21/16.
 */
public interface FieldParser {
	Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued);
}
