package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by stefanos on 20/6/2017.
 */
@Transactional
@Component
public abstract class AbstractGenericService<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractGenericService.class);
    protected final Class<T> typeParameterClass;
    @Autowired
    public SearchService searchService;
    @Autowired
    public ResourceService resourceService;
    @Autowired
    public ResourceTypeService resourceTypeService;
    @Autowired
    public ParserService parserPool;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;
    protected ResourceType resourceType;
    private List<String> browseBy;
    private Map<String, String> labels;

    public AbstractGenericService(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    public abstract String getResourceType();

    @PostConstruct
    void init() {
        resourceType = resourceTypeService.getResourceType(getResourceType());
        Set<String> browseSet = new HashSet<>();
        Map<String, Set<String>> sets = new HashMap<>();
        labels = new HashMap<>();
        labels.put("resourceType", "Resource Type");
        for (IndexField f : resourceTypeService.getResourceTypeIndexFields(getResourceType())) {
            sets.putIfAbsent(f.getResourceType().getName(), new HashSet<>());
            labels.put(f.getName(), f.getLabel());
            if (f.getLabel() != null) {
                sets.get(f.getResourceType().getName()).add(f.getName());
            }
        }
        boolean flag = true;
        for (Map.Entry<String, Set<String>> entry : sets.entrySet()) {
            if (flag) {
                browseSet.addAll(entry.getValue());
                flag = false;
            } else {
                browseSet.retainAll(entry.getValue());
            }
        }
        browseBy = new ArrayList<>();
        browseBy.addAll(browseSet);
        browseBy.add("resourceType");
        logger.info("Generated generic service for " + getResourceType() + "[" + getClass().getSimpleName() + "]");
    }

    protected Browsing<T> cqlQuery(FacetFilter filter) {
        filter.setResourceType(getResourceType());
        return convertToBrowsing(searchService.query(filter));
    }

    protected Browsing<T> getResults(FacetFilter filter) {
        Browsing<T> browsing;
        filter.setResourceType(getResourceType());
        try {
            browsing = convertToBrowsing(searchService.search(filter));
        } catch (UnknownHostException e) {
            logger.error("getResults", e);
            throw new ServiceException(e);
        }
        return browsing;
    }

    private Browsing<T> convertToBrowsing(@NotNull Paging<Resource> paging) {
        List<T> results = paging.getResults()
                .parallelStream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, labels);
    }


    protected Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category) {
        Map<String, List<T>> result = new HashMap<>();

        filter.setResourceType(getResourceType());
        Map<String, List<Resource>> resources;
        try {
            resources = searchService.searchByCategory(filter, category);
            for (Map.Entry<String, List<Resource>> bucket : resources.entrySet()) {
                List<T> bucketResults = new ArrayList<>();
                for (Resource res : bucket.getValue()) {
                    bucketResults.add(parserPool.deserialize(res, typeParameterClass));
                }
                result.put(bucket.getKey(), bucketResults);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error", e);
            throw new ServiceException(e);
        }
    }


    protected List<String> getBrowseBy() {
        return browseBy;
    }

    public void setBrowseBy(List<String> browseBy) {
        this.browseBy = browseBy;
    }
}
