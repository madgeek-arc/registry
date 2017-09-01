package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.domain.index.IndexField;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by stefanos on 20/6/2017.
 */

@Service("genericService")
abstract public class AbstractGenericService<T> {

    private Logger logger = Logger.getLogger(AbstractGenericService.class);

    @Autowired
    public SearchService searchService;

    @Autowired
    public ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    public ParserService parserPool;

    public final Class<T> typeParameterClass;

    private List<String> browseBy;

    public AbstractGenericService(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    public abstract String getResourceType();

    @PostConstruct
    void init() {
        Set<String> browseSet = new HashSet<>();
        Map<String,Set<String>> sets = new HashMap<>();
        for (IndexField f : resourceTypeService.getResourceTypeIndexFields(getResourceType())) {
            sets.putIfAbsent(f.getResourceType().getName(), new HashSet<>());
            if(f.getLabel() != null) {
                sets.get(f.getResourceType().getName()).add(f.getName());
            }
            //if(f.getLabel() != null) browseSet.add(f.getName());
            //System.out.println(f.getName() + " " + f.getResourceType().getName());
        }
        boolean flag = true;
        for(Map.Entry<String,Set<String>> entry : sets.entrySet()) {
            if(flag) {
                browseSet.addAll(entry.getValue());
                flag = false;
            } else {
                browseSet.retainAll(entry.getValue());
            }
        }
        browseBy = new ArrayList<>();
        browseBy.addAll(browseSet);
        browseBy.add("resourceType");
    }

    @SuppressWarnings("unchecked")
    public Browsing<T> getResults(FacetFilter filter) {
        List<Order<T>> result = new ArrayList<>();
        List<Future<T>> futureResults;
        Paging paging;
        filter.setResourceType(getResourceType());
        Browsing<T> browsing;
        Occurrences overall;
        List<Facet> facetsCollection;
        try {
            paging = searchService.search(filter);
            futureResults = new ArrayList<>(paging.getResults().size());
            int index = 0;
            for(Object res : paging.getResults()) {
                Resource resource = (Resource) res;
                futureResults.add(index,parserPool.serialize(resource,typeParameterClass));
                index++;
            }
            overall = paging.getOccurrences();
            facetsCollection = createFacetCollection(overall);
            for(Future<T> res : futureResults) {
                result.add(new Order(index,res.get()));
            }
        } catch (UnknownHostException | InterruptedException | ExecutionException e ) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        browsing = new Browsing(paging.getTotal(), filter.getFrom(), filter.getFrom() + result.size(), result, facetsCollection);
        return browsing;
    }


    public Map<String,List<T>> getResultsGrouped(FacetFilter filter, String category) {
        Map<String,List<T>> result = new HashMap<>();

        filter.setResourceType(getResourceType());
        Map<String,List<Resource>> resources;
        try {
            resources = searchService.searchByCategory(filter,category);
            for(Map.Entry<String,List<Resource>> bucket : resources.entrySet()) {
                List<T> bucketResults = new ArrayList<>();
                for(Resource res : bucket.getValue()) {
                    bucketResults.add(parserPool.serialize(res,typeParameterClass).get());
                }
                result.put(bucket.getKey(),bucketResults);
            }
            return result;
        } catch (Exception e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
    }
    /**
     * Counts the total number of Documents per Facet
     * @param overall
     * @return a List of facets.
     */
    List<Facet> createFacetCollection(Occurrences overall) {
        List<Facet> facetsCollection = new ArrayList<>();

        Map<String,String> labels = new HashMap<>();

        for (IndexField f : resourceTypeService.getResourceTypeIndexFields(getResourceType())) {
            labels.put(f.getName(),f.getLabel());
        }
        labels.put("resourceType","Resource Type");
        for (Map.Entry<String,String> label : labels.entrySet()) {
            Facet singleFacet = new Facet();

            singleFacet.setField(label.getKey());
            singleFacet.setLabel(label.getValue());

            List<Value> values = new ArrayList<>();
            Map<String, Integer> subMap = overall.getValues().get(label.getKey());
            if (subMap == null)
                continue;
            for (Map.Entry<String, Integer> pair2 : subMap.entrySet()) {
                Value value = new Value();
                value.setValue(pair2.getKey());
                value.setCount(pair2.getValue());
                values.add(value);
            }

            Collections.sort(values);
            Collections.reverse(values);
            singleFacet.setValues(values);

            if (singleFacet.getValues().size() > 0)
                facetsCollection.add(singleFacet);
        }
        return facetsCollection;
    }

    public List<String> getBrowseBy() {
        return browseBy;
    }

    public void setBrowseBy(List<String> browseBy) {
        this.browseBy = browseBy;
    }
}
