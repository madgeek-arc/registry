package eu.openminted.registry.core.index;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by antleb on 5/21/16.
 */
public interface FieldParser {
    static Set<Object> parseField(String fieldType, String typeValue) {
        Set<Object> values = new HashSet<>();
        if (!StringUtils.isEmpty(typeValue)) {
            switch (fieldType) {
                case "java.lang.String":
                    values.add(typeValue);
                    break;
                case "java.lang.Integer":
                case "java.lang.Long":
                    values.add(Long.parseLong(typeValue));
                    break;
                case "java.lang.Float":
                case "java.lang.Double":
                    values.add(Double.parseDouble(typeValue));
                    break;
                case "java.util.Date":
                    values.add(Date.from(Instant.ofEpochMilli(Long.parseLong(typeValue))));
                    break;
                case "java.lang.Boolean":
                    values.add(Boolean.parseBoolean(typeValue));
                    break;
            }
        }
        return values;
    }

    Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued);
}
