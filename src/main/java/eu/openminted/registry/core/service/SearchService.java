package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Paging;

import java.io.IOException;
import java.util.ArrayList;


public interface SearchService {
	Paging search(String resourceType, String cqlQuery, int from, int to, String[] browseBy) throws ServiceException;
}
