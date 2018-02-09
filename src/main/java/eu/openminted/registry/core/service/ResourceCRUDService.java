package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.exception.ResourceNotFoundException;

/**
 * Created by stefanos on 15-Nov-16.
 */
public interface ResourceCRUDService<T> {

    /**
     * Returns the resource.
     * @param id of the resource in the index.
     * @return the resource.
     */
    T get(String id);

    /**
     * Returns all resource.
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing getAll(FacetFilter filter);

    /**
     * Returns all resource of a user.
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing getMy(FacetFilter filter);

    /**
     * Add a new resource.
     * @param resource to be added
     * @return the id of the inserted resource.
     */
    T add(T resource);

    /**
     * Update the resource.
     * @param resource to be updated.
     * @return the updated resource.
     */
    T update(T resource) throws ResourceNotFoundException;

    /**
     * Delete the resource.
     * @param resource to be deleted.
     */
    void delete(T resource) throws ResourceNotFoundException;

}