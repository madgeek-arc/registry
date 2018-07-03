package eu.openminted.registry.core.index;

import com.jayway.jsonpath.JsonPath;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class JSONFieldParser implements FieldParser {
	
	public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

		Set<Object> response = new HashSet<Object>();
		if(isMultiValued){
			List<String> answers = JsonPath.read(payload, path);
			for(String answer:answers){
				FieldParser.parseField(fieldType,answer,response);
			}
		}else{
			String answer = JsonPath.read(payload, path);
			FieldParser.parseField(fieldType,answer,response);
		}
		return response;
	}
}
