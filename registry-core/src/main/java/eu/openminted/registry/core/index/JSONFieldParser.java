package eu.openminted.registry.core.index;

import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JSONFieldParser implements FieldParser {
	
	public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

		Set<Object> response = null;
		if(isMultiValued){
			List<String> answers = JsonPath.read(payload, path);
			if (answers != null) {
				response = answers
						.stream()
						.map(answer -> FieldParser.parseField(fieldType,answer))
						.flatMap(Collection::stream)
						.collect(Collectors.toSet());
			}
		}else{
			Object answer = JsonPath.read(payload + "", path);
			if (answer != null) {
				response = FieldParser.parseField(fieldType, answer.toString());
			}
		}
		return response;
	}
}
