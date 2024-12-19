package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by stefanos on 20/6/2017.
 */
@Transactional
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
    private final ResourceTypeInfo resourceTypeInfo;

    protected AbstractGenericService(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
        resourceTypeInfo = new ResourceTypeInfo();
    }

    public abstract String getResourceTypeName();

    public ResourceType getResourceType() {
        return resourceTypeInfo.getResourceType();
    }

    @PostConstruct
    void init() {
        logger.info("Generated generic service for {} [{}]", getResourceTypeName(), getClass().getSimpleName());
    }

    protected Browsing<T> getResults(FacetFilter filter) {
        Browsing<T> browsing;
        filter.setResourceType(getResourceTypeName());
        try {
            browsing = convertToBrowsing(searchService.search(filter));
        } catch (Exception e) {
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
        return new Browsing<>(paging, results, resourceTypeInfo.getLabels());
    }


    protected Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category) {
        Map<String, List<T>> result = new HashMap<>();

        filter.setResourceType(getResourceTypeName());
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
        return resourceTypeInfo.getBrowseBy();
    }

    public void setBrowseBy(List<String> browseBy) {
        resourceTypeInfo.setBrowseBy(browseBy);
    }

    private class ResourceTypeInfo {

        private ResourceType resourceType;
        private List<String> browseBy;
        private Map<String, String> labels;

        private ResourceTypeInfo() {
        }

        public void init() {
            if (resourceType == null) {
                resourceType = resourceTypeService.getResourceType(getResourceTypeName());
                Set<String> browseSet = new HashSet<>();
                Map<String, Set<String>> sets = new HashMap<>();
                labels = new HashMap<>();
                labels.put("resourceType", "Resource Type");
                for (IndexField f : resourceTypeService.getResourceTypeIndexFields(getResourceTypeName())) {
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
            }
        }

        public ResourceType getResourceType() {
            init();
            return resourceType;
        }

        public List<String> getBrowseBy() {
            init();
            return browseBy;
        }

        public void setBrowseBy(List<String> browseBy) {
            init();
            this.browseBy = browseBy;
        }

        public Map<String, String> getLabels() {
            init();
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            init();
            this.labels = labels;
        }
    }
}
