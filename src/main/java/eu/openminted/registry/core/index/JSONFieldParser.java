package eu.openminted.registry.core.index;

import org.apache.log4j.Logger;

import com.jayway.jsonpath.JsonPath;

/**
 * Created by antleb on 5/21/16.
 */
public class JSONFieldParser implements FieldParser {
	
	
	public Object parse(String payload, String fieldType, String path) {
		return "json field value";
	}
}
