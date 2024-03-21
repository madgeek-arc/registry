package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Resource;

import java.util.Arrays;

/**
 * Created by stefanos on 4/7/2017.
 */
public interface ParserService {

    <T> T deserialize(Resource resource, Class<T> returnType);

    String serialize(Object resource, ParserServiceTypes mediaType);

    enum ParserServiceTypes {
        JSON("json"),
        XML("xml");

        private final String type;

        ParserServiceTypes(final String type) {
            this.type = type;
        }

        public static ParserServiceTypes fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(ParserServiceTypes.values())
                    .filter(v -> v.type.equalsIgnoreCase(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

        public String getKey() {
            return type;
        }
    }
}

