package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

@RestController
public class SearchController {

	@Autowired
	private SearchService searchService;
	
	@RequestMapping(value = "/search/{name}/{query}", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<Paging> search(
			@PathVariable("name") String name,
			@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
			@RequestParam(value = "from", required = false, defaultValue = "0") int from,
			@RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity,
			@RequestParam(value = "browseBy", required = false, defaultValue = "") String[] browseBy
	) throws ServiceException {
		FacetFilter filter = new FacetFilter(keyword,name,from,quantity,new HashMap<>(), Arrays.asList(browseBy), null);
		try {
			return new ResponseEntity<>(searchService.search(filter), HttpStatus.OK);
		} catch (UnknownHostException e) {
			throw new ServiceException(e);
		}
	}

	@RequestMapping(value = "/search/cql/{resourceType}/{query}/", method = RequestMethod.GET)
	public ResponseEntity<Paging> cql(@PathVariable("query") String query,
									   @PathVariable("resourceType") String resourceType,
									   @RequestParam(value = "from", required = false, defaultValue = "0") int from,
									   @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity,
									   @RequestParam(value = "sortBy", required = false, defaultValue = "") String sortBy,
									   @RequestParam(value = "sortByType", required = false, defaultValue = "ASC")String sortByType){
		if(sortByType.equals("DESC") || sortByType.equals("ASC"))
			return new ResponseEntity<>(searchService.cqlQuery(query, resourceType,	quantity, from, sortBy, sortByType), HttpStatus.OK);
		else
			throw new ServiceException("Unsupported order by type");
	}
}
