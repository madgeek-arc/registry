package eu.openminted.registry.core.service;

import eu.dnetlib.functionality.index.cql.CqlTranslator;
import eu.openminted.registry.core.domain.Occurencies;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.z3950.zing.cql.CQLParseException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		String url = this.url.concat("/" + resourceType + "/");
		solrClient = new HttpSolrClient(url);
		SolrQuery sq = new SolrQuery();
		sq.setStart(from);
		sq.set("df", "payload");

		String solrQuery = null;
		try {
			solrQuery = translator.toLucene(cqlQuery);
		} catch (CQLParseException e) {
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		}

		sq.setQuery(solrQuery);

		if (browseBy.length != 0) {
			sq.setFacet(true);
			sq.setFacetMinCount(1);
			for (int i = 0; i < browseBy.length; i++) {
				sq.addFacetField(browseBy[i]);
			}
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
		Map<String, Map<String, Integer>> values = new HashMap<String, Map<String, Integer>>();
		Occurencies occurencies = new Occurencies();
		if (browseBy.length != 0) {
			for (int j = 0; j < facetFields.size(); j++) {
				Map<String, Integer> subMap = new HashMap<String, Integer>();
				for (int i = 0; i < facetFields.get(j).getValueCount(); i++) {
					subMap.put(facetFields.get(j).getValues().get(i).getName(), Integer.parseInt(facetFields.get(j).getValues().get(i).getCount() + ""));
				}
				values.put(facetFields.get(j).getName(), subMap);
			}
			occurencies.setValues(values);
		}
		if (docs == null || docs.size() == 0) {
			paging = new Paging(0, 0, 0, new ArrayList<>(), new Occurencies());
		} else {
			if (to == 0) {
				to = docs.size();
			}
			ArrayList<Resource> results = new ArrayList<>();

			for (SolrDocument doc : docs.subList(0, Math.min(10, docs.size()))) {
				results.add(new Resource((String) doc.get("id"), (String) doc.get("resourcetype"), null, (String) doc.get("payload"), null));
			}

//			for(int i=0;i<10 && i < docs.size();i++){
//
//				// TODO load from db when caching is ready or add fields to index
//				results.add(new Resource((String) docs.get(i).get("id"), "resourcetype", null, (String) docs.get(i).get("payload"), null));
//
////				results.add(docs.get(i));
//			}

			paging = new Paging((int) docs.getNumFound(), from, from + results.size() - 1, results, occurencies);
		}

		return paging;
	}
}
