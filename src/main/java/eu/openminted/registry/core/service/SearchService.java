package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Paging;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import eu.openminted.registry.core.domain.Resource;
import org.elasticsearch.index.query.BoolQueryBuilder;


public interface SearchService {

	Paging search(String resourceType, BoolQueryBuilder qBuilder, int from, int to, String[] browseBy) throws ServiceException, UnknownHostException;

	Resource searchId(String resourceType, String id) throws ServiceException, UnknownHostException;
}
