package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import java.net.UnknownHostException;
import eu.openminted.registry.core.domain.Resource;



public interface SearchService {

	Paging search(FacetFilter filter) throws ServiceException, UnknownHostException;

	Paging searchKeyword(String resourceType, String keyword) throws ServiceException, UnknownHostException;

	Resource searchId(String resourceType, String id) throws ServiceException, UnknownHostException;
}
