package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

@RestController
public class SearchController {

	@Autowired
	private SearchService searchService;
	
	@RequestMapping(value = "/search/{name}/{query}", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<Paging> getResourceTypeByName(
			@PathVariable("name") String name,
			@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
			@RequestParam(value = "from", required = false, defaultValue = "0") int from,
			@RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity,
			@RequestParam(value = "browseBy", required = false, defaultValue = "") String[] browseBy
	) throws ServiceException {
		FacetFilter filter = new FacetFilter(keyword,name,from,quantity,new HashMap<>(), Arrays.asList(browseBy));
		try {
			return new ResponseEntity<>(searchService.search(filter), HttpStatus.OK);
		} catch (UnknownHostException e) {
			throw new ServiceException(e);
		}
	}
}
