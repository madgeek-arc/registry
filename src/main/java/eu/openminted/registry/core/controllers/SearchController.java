package eu.openminted.registry.core.controllers;

import java.io.IOException;

import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.z3950.zing.cql.CQLParseException;

import eu.dnetlib.functionality.index.cql.CqlTranslator;
import eu.dnetlib.functionality.index.cql.CqlTranslatorImpl;

@RestController
public class SearchController {

	@Autowired
	private SearchService searchService;

	@RequestMapping(value = "/search/{name}/{query}", method = RequestMethod.GET, headers = "Accept=application/json")
	public Paging getResourceTypeByName(@PathVariable("query") String query, @PathVariable("name") String name) {

		try {
			return searchService.search(name, query);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CQLParseException e) {
			e.printStackTrace();
		} catch (SolrServerException e) {
			e.printStackTrace();
		}

		return null;
	}
}
