package eu.openminted.registry.core.index;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by antleb on 5/21/16.
 */
public class JSONFieldParser implements FieldParser {
	
	public Set<Object> parse(String payload, String fieldType, String path) {

		Set<Object> response = new HashSet<Object>();
		response.add("yo");
		return response;
	}
}
