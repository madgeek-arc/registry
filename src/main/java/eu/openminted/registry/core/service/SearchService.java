package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Paging;

import java.io.IOException;

/**
 * Created by antleb on 7/15/16.
 */
public interface SearchService {
	Paging search(String resourceType, String cqlQuery, int from, int to, String browseBy) throws ServiceException;
}
