package eu.openminted.registry.core.index;

import java.sql.Date;
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
				if(fieldType.equals("java.lang.String")){
	            	response.add((String)answer);
	            }else if(fieldType.equals("java.lang.Integer")){
	            	response.add(Integer.parseInt(answer));
	            }else if(fieldType.equals("java.lang.Float")){
	            	response.add(Float.parseFloat(answer));
	            }else if(fieldType.equals("java.lang.Date")){
	            	response.add(Date.valueOf(answer));
	            }else if (fieldType.equals("java.lang.Boolean")){
					response.add(Boolean.parseBoolean(answer));
				}
			}
		}else{
			if(fieldType.equals("java.lang.String")){
            	response.add((String)JsonPath.read(payload, path));
            }else if(fieldType.equals("java.lang.Integer")){
            	response.add(Integer.parseInt(JsonPath.read(payload, path)));
            }else if(fieldType.equals("java.lang.Float")){
            	response.add(Float.parseFloat(JsonPath.read(payload, path)));
            }else if(fieldType.equals("java.util.Date")){
            	response.add(Date.valueOf((String)JsonPath.read(payload, path)));
            }else if (fieldType.equals("java.lang.Boolean")){
				response.add(Boolean.parseBoolean(JsonPath.read(payload, path)));
			}
		}
		return response;
	}
}
