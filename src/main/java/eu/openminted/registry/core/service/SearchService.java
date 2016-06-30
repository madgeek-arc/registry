package eu.openminted.registry.core.service;

import eu.dnetlib.functionality.index.cql.CqlTranslator;
import eu.openminted.registry.core.domain.Paging;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.z3950.zing.cql.CQLParseException;

import javax.annotation.PostConstruct;
import java.io.IOException;

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

		solrClient = new HttpSolrClient(url);
	}

	public Paging search(String resourceType, String cqlQuery) throws IOException, CQLParseException, SolrServerException {
		Paging paging;

		String url = this.url.concat(resourceType+"/");
		SolrQuery sq = new SolrQuery();

		String solrQuery = translator.toLucene(cqlQuery);

		sq.setQuery(solrQuery);

		QueryResponse rsp = solrClient.query(sq);
		SolrDocumentList docs = rsp.getResults();

		paging = new Paging(0, 0, 0, null);


		return paging;
	}
}
