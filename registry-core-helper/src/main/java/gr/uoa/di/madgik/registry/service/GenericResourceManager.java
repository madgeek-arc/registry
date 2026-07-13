/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.exception.UnsupportedSearchParameterException;
import gr.uoa.di.madgik.registry.utils.LoggingUtils;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Service
public class GenericResourceManager implements GenericResourceService {

    private static final Logger logger = LoggerFactory.getLogger(GenericResourceManager.class);

    protected final SearchService searchService;
    protected final ResourceService resourceService;
    protected final ResourceTypeService resourceTypeService;
    protected final VersionService versionService;
    protected final ParserService parserPool;
    protected final FacetLabelService facetLabelService;
    protected final ResourceValidator validator;

    protected GenericResourceManager(SearchService searchService,
                                     ResourceService resourceService,
                                     ResourceTypeService resourceTypeService,
                                     VersionService versionService,
                                     ParserService parserPool,
                                     FacetLabelService facetLabelService,
                                     @Autowired(required = false) ResourceValidator validator) {
        this.searchService = searchService;
        this.resourceService = resourceService;
        this.resourceTypeService = resourceTypeService;
        this.versionService = versionService;
        this.parserPool = parserPool;
        this.facetLabelService = facetLabelService;
        this.validator = validator;
    }

    @Override
    public <T> T get(String resourceTypeName, SearchService.KeyValue... keyValues) {
        Resource res = searchService.searchFields(resourceTypeName, keyValues);
        if (res == null) {
            String id = keyValues.length > 0 ? keyValues[0].getValue() : null;
            throw new ResourceNotFoundException(id, resourceTypeName);
        }
        return (T) parserPool.deserialize(res, getClassFromResourceType(resourceTypeName));
    }

    @Override
    public <T> List<ScoredResult<T>> recommend(FacetFilter filter, String id) {
        String resourceTypeName = filter.getResourceType();
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        return searchService.recommend(filter, new SearchService.KeyValue(resolveSinglePrimaryKeyField(resourceTypeName), id))
                .stream()
                .map(scored -> scored.<T>map(resource -> (T) parserPool.deserialize(resource, clazz)))
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<ScoredResult<T>> recommendByKey(FacetFilter filter, Map<String, String> keyValues) {
        String resourceTypeName = filter.getResourceType();
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        Resource reference = searchResourceByKey(resourceTypeName, keyValues, true);
        return searchService.recommend(filter, reference)
                .stream()
                .map(scored -> scored.<T>map(resource -> (T) parserPool.deserialize(resource, clazz)))
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<ScoredResult<T>> recommend(FacetFilter filter, T resource) {
        String resourceTypeName = filter.getResourceType();
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        Resource rawResource = new Resource();
        rawResource.setResourceTypeName(resourceTypeName);
        rawResource.setResourceType(resourceType);
        rawResource.setPayload(serialize(resource, resourceType));
        rawResource.setIndexedFields(resourceService.getIndexedFields(rawResource));
        return searchService.recommend(filter, rawResource)
                .stream()
                .map(scored -> scored.<T>map(r -> (T) parserPool.deserialize(r, clazz)))
                .collect(Collectors.toList());
    }

    @Override
    public <T> T add(String resourceTypeName, T resource) {
        return add(resourceTypeName, resource, true);
    }

    @Override
    public <T> T add(String resourceTypeName, T resource, boolean validate) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null) {
            throw ResourceNotFoundException.unknownResourceType(resourceTypeName);
        }
        runValidation(resource, resourceTypeName, validate);
        String payload = serialize(resource, resourceType);
        SearchService.KeyValue[] keyValues = extractPrimaryKeys(resourceType, payload);
        if (searchResource(resourceTypeName, keyValues) != null) {
            throw new ResourceAlreadyExistsException(
                    Arrays.stream(keyValues)
                            .map(kv -> kv.getField() + "=" + kv.getValue())
                            .collect(Collectors.joining(",")),
                    resourceTypeName);
        }
        Resource res = new Resource();
        res.setResourceTypeName(resourceTypeName);
        res.setResourceType(resourceType);
        res.setPayload(payload);
        logger.info("adding : [resourceType={}] : [body={}]", resourceTypeName, resource);
        resourceService.addResource(res);
        return resource;
    }

    @Override
    public <T> boolean exists(String resourceTypeName, T resource) { // TODO: do not get, just check
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        String payload = serialize(resource, resourceType);
        return searchResource(resourceTypeName, extractPrimaryKeys(resourceType, payload)) != null;
    }

    @Override
    public <T> Map<String, String> getPrimaryKeyValues(String resourceTypeName, T resource) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null) {
            throw ResourceNotFoundException.unknownResourceType(resourceTypeName);
        }
        String payload = serialize(resource, resourceType);
        SearchService.KeyValue[] keyValues = extractPrimaryKeys(resourceType, payload);
        Map<String, String> result = new LinkedHashMap<>();
        for (SearchService.KeyValue keyValue : keyValues) {
            result.put(keyValue.getField(), keyValue.getValue());
        }
        return result;
    }

    @Override
    public <T> T update(String resourceTypeName, T resource) {
        return update(resourceTypeName, resource, true);
    }

    @Override
    public <T> T update(String resourceTypeName, T resource, boolean validate) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        String payload = serialize(resource, resourceType);
        SearchService.KeyValue[] keyValues = extractPrimaryKeys(resourceType, payload);

        runValidation(resource, resourceTypeName, validate);

        Resource res = Optional.ofNullable(searchResource(resourceTypeName, keyValues))
                .orElseThrow(() -> new ResourceNotFoundException(
                        Arrays.stream(keyValues)
                                .map(kv -> kv.getField() + "=" + kv.getValue())
                                .collect(Collectors.joining(",")),
                        resourceTypeName));

        String logId = Arrays.stream(keyValues).map(SearchService.KeyValue::getValue).collect(Collectors.joining(","));

        // Compared before mutating res: res is a managed entity, so setPayload() below would
        // otherwise corrupt this "previous payload" read if taken afterwards. Skipping here avoids
        // a no-op Version row and no-op modifiedBy/listener churn for a submission that changes nothing.
        if (Objects.equals(res.getPayload(), payload)) {
            logger.debug("Skipping update for resource type '{}' id '{}': submitted payload is unchanged", resourceTypeName, logId);
            return resource;
        }

        res.setPayload(payload);
        logger.info(LoggingUtils.updateResource(resourceTypeName, logId, resource));
        resourceService.updateResource(res);
        return resource;
    }

    @Override
    public <T> T delete(String resourceTypeName, String id) {
        Resource res = searchResource(resourceTypeName, id, true);
        logger.info(LoggingUtils.deleteResource(resourceTypeName, id, res));
        resourceService.deleteResource(res.getId());
        return (T) parserPool.deserialize(res, getClassFromResourceType(resourceTypeName));
    }

    @Override
    public <T> T deleteByKey(String resourceTypeName, Map<String, String> keyValues) {
        Resource res = searchResourceByKey(resourceTypeName, keyValues, true);
        logger.info(LoggingUtils.deleteResource(resourceTypeName, joinKeyValues(keyValues), res));
        resourceService.deleteResource(res.getId());
        return (T) parserPool.deserialize(res, getClassFromResourceType(resourceTypeName));
    }

    @Override
    public <T> T get(String resourceTypeName, String id) {
        Resource res = searchResource(resourceTypeName, id, true);
        return (T) parserPool.deserialize(res, getClassFromResourceType(res.getResourceTypeName()));
    }

    @Override
    public <T> T getByKey(String resourceTypeName, Map<String, String> keyValues) {
        Resource res = searchResourceByKey(resourceTypeName, keyValues, true);
        return (T) parserPool.deserialize(res, getClassFromResourceType(res.getResourceTypeName()));
    }

    @Override
    public <T> T get(String resourceTypeName, Version version) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null) {
            throw ResourceNotFoundException.unknownResourceType(resourceTypeName);
        }
        return (T) parserPool.deserialize(version.getPayload(), resourceType.getPayloadType(),
                getClassFromResourceType(resourceTypeName));
    }

    @Override
    public <T> Paging<T> getResults(FacetFilter filter) {
        Paging<T> results = convertToPaging(searchService.search(filter), filter.getResourceType());
        facetLabelService.enrichFacetLabels(results.getFacets(), filter.getResourceType());
        return results;
    }

    @Override
    public <T> Paging<T> getSemanticResults(FacetFilter filter) {
        Paging<T> results = convertToPaging(searchService.semanticSearch(filter), filter.getResourceType());
        facetLabelService.enrichFacetLabels(results.getFacets(), filter.getResourceType());
        return results;
    }

    @Override
    public <T> Paging<T> getHybridResults(FacetFilter filter) {
        Paging<T> results = convertToPaging(searchService.hybridSearch(filter), filter.getResourceType());
        facetLabelService.enrichFacetLabels(results.getFacets(), filter.getResourceType());
        return results;
    }

    @Override
    public <T> Paging<HighlightedResult<T>> getHighlightedResults(FacetFilter filter) {
        Paging<HighlightedResult<T>> results = convertToPagingWithHighlights(
                searchService.searchWithHighlights(filter), filter.getResourceType());
        facetLabelService.enrichFacetLabels(results.getFacets(), filter.getResourceType());
        return results;
    }

    @Override
    public <T> Paging<HighlightedResult<T>> getHybridHighlightedResults(FacetFilter filter) {
        Paging<HighlightedResult<T>> results = convertToPagingWithHighlights(
                searchService.hybridSearchWithHighlights(filter), filter.getResourceType());
        facetLabelService.enrichFacetLabels(results.getFacets(), filter.getResourceType());
        return results;
    }

    @Override
    public <T> Paging<T> getResults(FacetFilter filter, UnaryOperator<List<Facet>> transformer) {
        Paging<T> paging = getResults(filter);
        paging.setFacets(transformer.apply(paging.getFacets()));
        return paging;
    }

    @Override
    public <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category) {
        Map<String, List<T>> result = new HashMap<>();
        Class<?> clazz = getClassFromResourceType(filter.getResourceType());
        Map<String, List<Resource>> resources = searchService.searchByCategory(filter, category);
        for (Map.Entry<String, List<Resource>> bucket : resources.entrySet()) {
            List<T> bucketResults = new ArrayList<>();
            for (Resource res : bucket.getValue()) {
                Class<?> resolvedClass = clazz != null ? clazz : getClassFromResourceType(res.getResourceTypeName());
                bucketResults.add((T) parserPool.deserialize(res, resolvedClass));
            }
            result.put(bucket.getKey(), bucketResults);
        }
        return result;
    }

    @Override
    public <T> T validate(String resourceTypeName, T resource) {
        if (validator == null) {
            return resource;
        }
        return validator.validate(resource, resourceTypeName);
    }

    @Override
    public Class<?> getClassFromResourceType(String resourceTypeName) {
        try {
            ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
            if (resourceType == null) {
                return null;
            }
            return Class.forName(resourceType.getProperty("class"));
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
            return Map.class;
        } catch (NullPointerException e) {
            logger.error("Class property is not defined", e);
            throw new ServiceException(
                    String.format("ResourceType [%s] does not have properties field", resourceTypeName));
        }
    }

    @Override
    public Resource searchResource(String resourceTypeName, String id, boolean throwOnNull) {
        Resource res = searchService.searchFields(resourceTypeName,
                new SearchService.KeyValue(resolveSinglePrimaryKeyField(resourceTypeName), id));
        if (throwOnNull) {
            return Optional.ofNullable(res)
                    .orElseThrow(() -> new ResourceNotFoundException(id, resourceTypeName));
        }
        return res;
    }

    @Override
    public Resource searchResource(String resourceTypeName, SearchService.KeyValue... keyValues) {
        return searchService.searchFields(resourceTypeName, keyValues);
    }

    @Override
    public Resource searchResourceByKey(String resourceTypeName, Map<String, String> keyValues, boolean throwOnNull) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null) {
            throw ResourceNotFoundException.unknownResourceType(resourceTypeName);
        }
        SearchService.KeyValue[] resolved = resolvePrimaryKeyValues(resourceType, keyValues);
        Resource res = searchService.searchFields(resourceTypeName, resolved);
        if (throwOnNull) {
            return Optional.ofNullable(res)
                    .orElseThrow(() -> new ResourceNotFoundException(joinKeyValues(keyValues), resourceTypeName));
        }
        return res;
    }

    private String resolveSinglePrimaryKeyField(String resourceTypeName) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null) {
            throw ResourceNotFoundException.unknownResourceType(resourceTypeName);
        }

        List<IndexField> primaryKeyFields = resourceType.getIndexFields().stream()
                .filter(IndexField::isPrimaryKey)
                .toList();
        if (primaryKeyFields.isEmpty()) {
            throw new ServiceException(
                    String.format("ResourceType [%s] has no primary key field defined", resourceTypeName));
        }
        if (primaryKeyFields.size() > 1) {
            throw new UnsupportedSearchParameterException(
                    "ResourceType '%s' has a composite primary key; path lookup by primary key is not supported."
                            .formatted(resourceTypeName));
        }
        return primaryKeyFields.getFirst().getName();
    }

    /**
     * Validates that {@code keyValues} contains exactly {@code resourceType}'s declared
     * primary-key fields — no more, no fewer — and converts it to the {@link SearchService.KeyValue}
     * array the search layer expects.
     */
    private SearchService.KeyValue[] resolvePrimaryKeyValues(ResourceType resourceType, Map<String, String> keyValues) {
        List<String> pkFieldNames = resourceType.getIndexFields().stream()
                .filter(IndexField::isPrimaryKey)
                .map(IndexField::getName)
                .toList();
        if (pkFieldNames.isEmpty()) {
            throw new ServiceException(
                    String.format("ResourceType [%s] has no primary key field defined", resourceType.getName()));
        }
        Set<String> expected = new HashSet<>(pkFieldNames);
        if (!expected.equals(keyValues.keySet())) {
            Set<String> missing = new HashSet<>(expected);
            missing.removeAll(keyValues.keySet());
            Set<String> unexpected = new HashSet<>(keyValues.keySet());
            unexpected.removeAll(expected);
            throw new UnsupportedSearchParameterException(
                    "ResourceType '%s' has primary key field(s) %s; missing=%s, unexpected=%s"
                            .formatted(resourceType.getName(), pkFieldNames, missing, unexpected));
        }
        return pkFieldNames.stream()
                .map(name -> new SearchService.KeyValue(name, keyValues.get(name)))
                .toArray(SearchService.KeyValue[]::new);
    }

    private static String joinKeyValues(Map<String, String> keyValues) {
        return keyValues.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

    public <T> List<T> convertToList(@NotNull List<Resource> resources, String resourceTypeName) {
        return deserializeResources(resources, resourceTypeName);
    }

    public <T> Paging<T> convertToPaging(@NotNull Paging<Resource> paging, String resourceTypeName) {
        return new Paging<>(paging, deserializeResources(paging.getResults(), resourceTypeName));
    }

    private String serialize(Object resource, ResourceType resourceType) {
        return parserPool.serialize(resource,
                ParserService.ParserServiceTypes.fromString(resourceType.getPayloadType()));
    }

    private SearchService.KeyValue[] extractPrimaryKeys(ResourceType resourceType, String payload) {
        List<IndexField> pkFields = resourceType.getIndexFields().stream()
                .filter(IndexField::isPrimaryKey)
                .toList();
        if (pkFields.isEmpty()) {
            throw new ServiceException(
                    String.format("ResourceType [%s] has no primary key field defined", resourceType.getName()));
        }
        return pkFields.stream()
                .map(f -> new SearchService.KeyValue(f.getName(),
                        parserPool.extractValue(payload, resourceType.getPayloadType(), f.getPath())))
                .toArray(SearchService.KeyValue[]::new);
    }

    private <T> void runValidation(T resource, String resourceTypeName, boolean validate) {
        if (validate && validator != null) {
            validator.validate(resource, resourceTypeName);
        }
    }

    private <T> List<T> deserializeResources(List<Resource> resources, String resourceTypeName) {
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        List<T> results = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource == null) continue;
            if (clazz == null) {
                clazz = getClassFromResourceType(resource.getResourceTypeName());
            }
            results.add((T) parserPool.deserialize(resource, clazz));
        }
        return results;
    }

    public <T> Paging<HighlightedResult<T>> convertToPagingWithHighlights(
            @NotNull Paging<HighlightedResult<Resource>> paging, String resourceTypeName) {
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        List<HighlightedResult<T>> results = new ArrayList<>();
        for (HighlightedResult<Resource> resource : paging.getResults()) {
            if (resource == null) continue;
            if (clazz == null) {
                clazz = getClassFromResourceType(resource.getResult().getResourceTypeName());
            }
            final Class<?> finalClass = clazz;
            results.add(resource.map(v -> (T) parserPool.deserialize(v, finalClass)));
        }
        return new Paging<>(paging, results);
    }

}
