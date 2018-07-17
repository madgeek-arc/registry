package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Resource;

/**
 * Created by stefanos on 4/7/2017.
 */
public interface ParserService {

    <T> T deserialize(Resource resource, Class<T> returnType);

    String serialize(Object resource, ParserServiceTypes mediaType);

    enum ParserServiceTypes {
        JSON, XML
    }
}

