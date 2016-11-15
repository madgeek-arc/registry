package eu.openminted.registry.core.service;

import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Occurencies;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service("searchService")
public class SearchServiceImpl implements SearchService {

    @Autowired
    Environment environment;

    @Autowired
    private ElasticConfiguration elastic;

    @Override
    public Paging search(String resourceType, BoolQueryBuilder qBuilder, int from, int to,
                         String[] browseBy) throws ServiceException {
        Client client = elastic.client();

        Paging paging;
        int quantity = to;
        if (to == 0) {
            quantity = 10;
        }


        SearchRequestBuilder search = client.prepareSearch(resourceType).setSearchType(SearchType.DFS_QUERY_AND_FETCH)
                .setQuery(qBuilder)
                .setFrom(from).setSize(quantity).setExplain(false);


        for (int i = 0; i < browseBy.length; i++) {
            search.addAggregation(AggregationBuilders.terms("by_" + browseBy[i]).field(browseBy[i]));
        }

        SearchResponse response = search.execute().actionGet();

        List<Resource> results = new ArrayList<Resource>();

        for (SearchHit hit : response.getHits().getHits()) {
            results.add(new Resource(hit.getSource().get("id").toString(), hit.getSource().get("resourceType").toString(), null, hit.getSource().get("payload").toString(), null));
        }

        Map<String, Map<String, Integer>> values = new HashMap<String, Map<String, Integer>>();
        Occurencies occurencies = new Occurencies();
        if (browseBy.length != 0) {

            for (int j = 0; j < browseBy.length; j++) {
                Map<String, Integer> subMap = new HashMap<String, Integer>();
                Terms terms = response.getAggregations().get("by_" + browseBy[j]);
                for (Bucket bucket : terms.getBuckets()) {
                    subMap.put(bucket.getKeyAsString(), Integer.parseInt(bucket.getDocCount() + ""));
                }
                values.put(browseBy[j], subMap);
            }
            occurencies.setValues(values);

        }
        if (response == null || response.getHits().getTotalHits() == 0) {
            paging = new Paging(0, 0, 0, new ArrayList<>(), new Occurencies());
        } else {
            if (to == 0) {
                to = from + quantity;
            }

            paging = new Paging((int) response.getHits().getTotalHits(), from, from + results.size(), results, occurencies);
        }

        return paging;
    }

    @Override
    public Resource searchId(String resourceType, String id) throws ServiceException, UnknownHostException {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        qBuilder.must(QueryBuilders.termsQuery("omtdid", id));
        Client client = elastic.client();

        SearchRequestBuilder search = client.prepareSearch(resourceType).setSearchType(SearchType.DFS_QUERY_AND_FETCH)
                .setQuery(qBuilder)
                .setSize(1).setExplain(false);

        SearchResponse response = search.execute().actionGet();
        if (response == null || response.getHits().totalHits() == 0) {
            return null;
        } else {
            SearchHit hit = response.getHits().getAt(0);
            return new Resource(hit.getSource().get("id").toString(), hit.getSource().get("resourceType").toString(), null, hit.getSource().get("payload").toString(), null);
        }
    }
}
