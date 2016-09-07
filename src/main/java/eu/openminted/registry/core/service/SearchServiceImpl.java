package eu.openminted.registry.core.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import eu.openminted.registry.core.domain.Resource;
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
public class SearchServiceImpl implements SearchService {

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

	@Override
	public Paging search(String resourceType, String cqlQuery, int from, int to, String[] browseBy) throws ServiceException {
		Paging paging;

		String url = this.url.concat("/"+resourceType+"/");
		solrClient = new HttpSolrClient(url);
		SolrQuery sq = new SolrQuery();

		String solrQuery = null;
		try {
			solrQuery = translator.toLucene(cqlQuery);
		} catch (CQLParseException e) {
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		}

		sq.setQuery(solrQuery);
		if(browseBy.length!=0){
			sq.setFacet(true);
			sq.setFacetMinCount(1);
			for(int i=0;i<browseBy.length;i++){
				sq.addFacetField(browseBy[i]);
			}
//			sq.addFacetField(browseBy);
			sq.setFacetLimit(-1);
			sq.setFacetMinCount(1);
			sq.setFacetSort("count");
		}
		QueryResponse rsp = null;
		try {
			rsp = solrClient.query(sq);
		} catch (SolrServerException e) {
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		}
		SolrDocumentList docs = rsp.getResults();
		List<FacetField> facetFields = rsp.getFacetFields();
		ArrayList<String> values = new ArrayList<String>();
		if(browseBy.length!=0){
			for(int j=0;j<facetFields.size();j++){
				for(int i=0;i<facetFields.get(j).getValueCount();i++){
					JSONObject attr = new JSONObject();
					attr.put("value", facetFields.get(j).getValues().get(i).getName());
					attr.put("count", facetFields.get(j).getValues().get(i).getCount());
					values.add(attr.toString());
				}
			}
		}
		if(docs==null || docs.size()==0){
			paging = new Paging(0, 0, 0, null,null);
		}else{
			if(to==0){
				to=docs.size();
			}
//			ArrayList<SolrDocument> results = new ArrayList<SolrDocument>();
			ArrayList<Resource> results = new ArrayList<>();

			for(int i=from;i<to;i++){

				// TODO load from db when caching is ready or add fields to index
				results.add(new Resource((String) docs.get(i).get("id"), "resourceType", null, (String) docs.get(i).get("payload"), null));

//				results.add(docs.get(i));
			}
			paging = new Paging(docs.size(),from,docs.size(),results,values);
		}
		
		return paging;
	}
}
