package eu.openminted.registry.core.service;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.z3950.zing.cql.CQLParseException;

import eu.dnetlib.functionality.index.cql.CqlTranslator;
import eu.openminted.registry.core.domain.Paging;

/**
 * Created by antleb on 6/30/16.
 */
@Service("searchService")
public class SearchService {

	private String url;
	private SolrClient solrClient;
	

	@Autowired
	Environment environment;

	@Autowired
	private CqlTranslator translator;

	@PostConstruct
	public void init() {
		url = environment.getProperty("solr.host", "http://83.212.121.189:8983/solr/");
	}

	public Paging search(String resourceType, String cqlQuery, int from, int to, String browseBy) throws IOException, CQLParseException, SolrServerException {
		Paging paging;
		if(!browseBy.equals("")){

		}
		
		String url = this.url.concat(resourceType+"/");
		solrClient = new HttpSolrClient(url);
		SolrQuery sq = new SolrQuery();
		
		String solrQuery = translator.toLucene(cqlQuery);

		sq.setQuery(solrQuery);
		if(!browseBy.equals("")){
			sq.setFacet(true);
			sq.setFacetMinCount(1);
			sq.addFacetField(browseBy);
			sq.setFacetLimit(-1);
			sq.setFacetMinCount(1);
			sq.setFacetSort("count");
		}
		QueryResponse rsp = solrClient.query(sq);
		SolrDocumentList docs = rsp.getResults();
		FacetField facetField = rsp.getFacetField(browseBy);
		ArrayList<String> values = new ArrayList<String>();
		if(!browseBy.equals("")){
			for(int i=0;i<facetField.getValueCount();i++){
				JSONObject attr = new JSONObject();
				attr.put("value", facetField.getValues().get(i).getName());
				attr.put("count", facetField.getValues().get(i).getCount());
				values.add(attr.toString());
			}
		}
		
		if(docs==null || docs.size()==0){
			paging = new Paging(0, 0, 0, null,null);
		}else{
			if(to==0){
				to=docs.size();
			}
			ArrayList<SolrDocument> results = new ArrayList<SolrDocument>();
							
			for(int i=from;i<to;i++){
				results.add(docs.get(i));
			}
			paging = new Paging(docs.size(),from,docs.size(),results,values);
		}

		
		return paging;
	}
}
