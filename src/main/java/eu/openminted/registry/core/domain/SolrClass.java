package eu.openminted.registry.core.domain;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrClass {

	String url = "http://83.212.121.189:8983/solr/";	
	
	
	
	public SolrDocumentList SolrClass(String resourceType, String query) throws IOException {
		
		url = url.concat(resourceType+"/");
		SolrClient solrClient = new HttpSolrClient(url);
		SolrQuery sq = new SolrQuery();
		sq.setQuery(query);
		
		try {
			QueryResponse rsp = solrClient.query(sq);
			SolrDocumentList docs = rsp.getResults();
			return docs;
		} catch (SolrServerException e) {
			return null;
		}
		
	}	
}
