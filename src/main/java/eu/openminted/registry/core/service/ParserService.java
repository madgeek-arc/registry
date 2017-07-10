package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Resource;
import java.util.concurrent.Future;

/**
 * Created by stefanos on 4/7/2017.
 */
public interface ParserService {

    <T> Future<T> serialize(Resource resource, Class<T> returnType);

    Future<String> deserialize(Object resource, ParserServiceTypes mediaType);

    enum ParserServiceTypes {
        JSON,XML
    }
}

