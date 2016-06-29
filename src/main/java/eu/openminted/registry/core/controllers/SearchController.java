package eu.openminted.registry.core.controllers;

import java.io.IOException;

import org.apache.solr.common.SolrDocumentList;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.z3950.zing.cql.CQLParseException;

import eu.dnetlib.functionality.index.cql.CqlTranslator;
import eu.dnetlib.functionality.index.cql.CqlTranslatorImpl;
import eu.openminted.registry.core.domain.SolrClass;

@RestController
public class SearchController {

	
	private CqlTranslator translator;
	
	
	@RequestMapping(value = "/search/{name}/{query}", method = RequestMethod.GET, headers = "Accept=application/json")
	public String getResourceTypeByName(@PathVariable("query") String query, @PathVariable("name") String name) {

		translator = new CqlTranslatorImpl();
		
		SolrClass solrClass = new SolrClass();
		SolrDocumentList result;
		try {
			result = solrClass.SolrClass(name, translator.toLucene(query) );
			return result.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "none";
		} catch (CQLParseException e) {
			// TODO Auto-generated catch block
			return "none2";
		}
		
		
		
		
	}
}
