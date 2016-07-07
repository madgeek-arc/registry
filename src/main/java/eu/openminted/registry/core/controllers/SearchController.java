package eu.openminted.registry.core.controllers;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.z3950.zing.cql.CQLParseException;

import eu.openminted.registry.core.domain.Tools;
import eu.openminted.registry.core.service.SearchService;

@RestController
public class SearchController {

	@Autowired
	private SearchService searchService;
	
	@RequestMapping(value = "/search/{name}/{query}/", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName(@PathVariable("query") String query, @PathVariable("name") String name) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,0,0,"")), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		

		return responseEntity;
	}
	
	
	@RequestMapping(value = "/search/{name}/{query}/", params = {"from"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName1(@PathVariable("query") String query, @PathVariable("name") String name, @RequestParam(value = "from") int from) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,from,0,"")), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}
	
	@RequestMapping(value = "/search/{name}/{query}/", params = {"to"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName2(@PathVariable("query") String query, @PathVariable("name") String name, @RequestParam(value = "to") int to ) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,0,to,"")), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}
	
	@RequestMapping(value = "/search/{name}/{query}/", params = {"from","to"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName3(@PathVariable("query") String query, @PathVariable("name") String name, @RequestParam(value="from") int from ,@RequestParam(value = "to") int to ) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,from,to,"")), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
		
	}
	
	@RequestMapping(value = "/search/{name}/{query}/",params = {"browseBy"} ,method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName(@PathVariable("query") String query, @PathVariable("name") String name, @RequestParam(value="browseBy") String browseBy) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,0,0,browseBy)), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}
	
	
	@RequestMapping(value = "/search/{name}/{query}/", params = {"from","browseBy"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName1(@PathVariable("query") String query, @PathVariable("name") String name, @RequestParam(value = "from") int from, @RequestParam(value = "browseBy") String browseBy) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,from,0,browseBy)), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}
	
	@RequestMapping(value = "/search/{name}/{query}/", params = {"to","browseBy"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName2(@PathVariable("query") String query, @PathVariable("name") String name, @RequestParam(value = "to") int to, @RequestParam(value = "browseBy") String browseBy ) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,0,to,browseBy)), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}
	
	@RequestMapping(value = "/search/{name}/{query}/", params = {"from","to","browseBy"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName3(@PathVariable("query") String query, @PathVariable("name") String name, @RequestParam(value="from") int from ,@RequestParam(value = "to") int to, @RequestParam(value="browseBy") String browseBy  ) {
		ResponseEntity<String> responseEntity;
		
		try {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(searchService.search(name, query,from,to,browseBy)), HttpStatus.OK);
			return responseEntity;
		} catch (IOException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (CQLParseException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SolrServerException e) {
			responseEntity = new ResponseEntity<String>("{\"error\":\""+e.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
		
	}
}
