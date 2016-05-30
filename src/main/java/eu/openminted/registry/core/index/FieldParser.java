package eu.openminted.registry.core.index;

/**
 * Created by antleb on 5/21/16.
 */
public interface FieldParser {
	Object parse(String payload, String fieldType, String path);
}
