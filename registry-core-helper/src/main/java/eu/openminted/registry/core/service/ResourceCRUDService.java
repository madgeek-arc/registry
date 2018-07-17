package eu.openminted.registry.core.service;

import org.springframework.security.core.Authentication;

/**
 * Created by stefanos on 15-Nov-16.
 */
public interface ResourceCRUDService<T, U extends Authentication> extends TransformerCRUDService<T, T, U> {

}