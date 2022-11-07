package eu.openminted.registry.core.index;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JSONFieldParser implements FieldParser {

    private static final Logger logger = LogManager.getLogger(JSONFieldParser.class);

    public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

        Set<Object> response = null;
        try {
            if (isMultiValued) {
                List<Object> answers = JsonPath.read(payload, path);
                if (answers != null) {
                    response = answers
                            .stream()
                            .map(answer -> FieldParser.parseField(fieldType, String.valueOf(answer)))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                }
            } else {
                Object answer = JsonPath.read(payload + "", path);
                if (answer != null) {
                    response = FieldParser.parseField(fieldType, answer.toString());
                }
            }
        } catch (PathNotFoundException e) {
            logger.debug(e);
        }
        return response;
    }
}
