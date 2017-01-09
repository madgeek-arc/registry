package eu.openminted.registry.core.index;

import java.sql.Date;
import java.util.Set;

/**
 * Created by antleb on 5/21/16.
 */
public interface FieldParser {
	Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued);

	static void parseField(String fieldType, String typeValue, Set<Object> values) {
		if(fieldType.equals("java.lang.String")){
			values.add((String)typeValue);
		}else if(fieldType.equals("java.lang.Integer")){
			values.add(Integer.parseInt(typeValue));
		}else if(fieldType.equals("java.lang.Float")){
			values.add(Float.parseFloat(typeValue));
		}else if(fieldType.equals("java.util.Date")){
			values.add(Date.valueOf(typeValue));
		}else if (fieldType.equals("java.lang.Boolean")){
			values.add(Boolean.parseBoolean(typeValue));
		}
	}
}
