package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;

public interface TransformerCRUDService<T, R, U extends Authentication> {
    /**
     * Returns the resource.
     *
     * @param id of the resource in the index.
     * @return the resource.
     */
    R get(String id);

    /**
     * Returns all resource.
     *
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing<R> getAll(FacetFilter filter, U authentication);

    /**
     * Returns all resource of a user.
     *
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing<R> getMy(FacetFilter filter, U authentication);

    /**
     * Add a new resource.
     *
     * @param resource to be added
     * @return the id of the inserted resource.
     */
    R add(T resource, U authentication);

    /**
     * Update the resource.
     *
     * @param resource to be updated.
     * @return the updated resource.
     */
    R update(T resource, U authentication) throws ResourceNotFoundException;

    /**
     * Delete the resource.
     *
     * @param resourceId to be deleted.
     */
    void delete(T resourceId) throws ResourceNotFoundException;
}
