package eu.openminted.registry.core.index;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jayway.jsonpath.JsonPath;


public class JSONFieldParser implements FieldParser {
	
	public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

		Set<Object> response = new HashSet<Object>();
		if(isMultiValued){
			List<String> answers = JsonPath.read(payload, path);
			for(String answer:answers){
				response.add(answer);
			}
		}else{
			response.add(JsonPath.read(payload, path));
		}
		return response;
	}
}
