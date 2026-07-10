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

package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Service("genericResourceService")
public class GenericResourceServiceImpl implements GenericResourceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SearchService searchService;
    private final ResourceTypeService resourceTypeService;
    private final String registryHost;

    public GenericResourceServiceImpl(RestTemplate restTemplate,
                                      ObjectMapper objectMapper,
                                      SearchService searchService,
                                      ResourceTypeService resourceTypeService,
                                      @Value("${registry.base}") String registryHost) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.searchService = searchService;
        this.resourceTypeService = resourceTypeService;
        this.registryHost = registryHost;
    }

    @Override
    public <T> T get(String resourceTypeName, SearchService.KeyValue... keyValues) {
        Resource resource = searchResource(resourceTypeName, keyValues);
        if (resource == null) {
            throw new ResourceNotFoundException(
                    keyValues.length == 0 ? "unknown" : keyValues[0].getValue(),
                    resourceTypeName
            );
        }
        return deserialize(resource, resourceTypeName);
    }

    @Override
    public <T> T get(String resourceTypeName, String id) {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                registryHost + "/records/" + resourceTypeName + "/" + id,
                Object.class
        );
        return convertBody(response.getBody(), resourceTypeName);
    }

    @Override
    public <T> T get(String resourceTypeName, Version version) {
        return deserializePayload(version.getPayload(), resourceTypeName);
    }

    @Override
    public <T> T getByKey(String resourceTypeName, Map<String, String> keyValues) {
        ResponseEntity<Object> response = restTemplate.getForEntity(buildKeyUri(resourceTypeName, keyValues), Object.class);
        return convertBody(response.getBody(), resourceTypeName);
    }

    @Override
    public <T> Paging<T> getResults(FacetFilter filter) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(buildBrowseUri(filter, null, false), Paging.class);
        Paging<?> paging = response.getBody() == null ? new Paging<>() : response.getBody();
        return convertPaging(paging, Objects.requireNonNull(filter.getResourceType()));
    }

    @Override
    public <T> Paging<T> getSemanticResults(FacetFilter filter) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(buildBrowseUri(filter, "semantic", false), Paging.class);
        Paging<?> paging = response.getBody() == null ? new Paging<>() : response.getBody();
        return convertPaging(paging, Objects.requireNonNull(filter.getResourceType()));
    }

    @Override
    public <T> Paging<T> getHybridResults(FacetFilter filter) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(buildBrowseUri(filter, "hybrid", false), Paging.class);
        Paging<?> paging = response.getBody() == null ? new Paging<>() : response.getBody();
        return convertPaging(paging, Objects.requireNonNull(filter.getResourceType()));
    }

    @Override
    public <T> Paging<T> getResults(FacetFilter filter, UnaryOperator<List<Facet>> transformer) {
        Paging<T> paging = getResults(filter);
        paging.setFacets(transformer.apply(paging.getFacets()));
        return paging;
    }

    @Override
    public <T> Paging<HighlightedResult<T>> getHighlightedResults(FacetFilter filter) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(buildBrowseUri(filter, null, true), Paging.class);
        Paging<?> paging = response.getBody() == null ? new Paging<>() : response.getBody();
        return convertHighlightedPaging(paging, Objects.requireNonNull(filter.getResourceType()));
    }

    @Override
    public <T> Paging<HighlightedResult<T>> getHybridHighlightedResults(FacetFilter filter) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(buildBrowseUri(filter, "hybrid", true), Paging.class);
        Paging<?> paging = response.getBody() == null ? new Paging<>() : response.getBody();
        return convertHighlightedPaging(paging, Objects.requireNonNull(filter.getResourceType()));
    }

    @Override
    public <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented by registry-starter-client");
    }

    @Override
    public <T> List<ScoredResult<T>> recommend(FacetFilter filter, String id) {
        ResponseEntity<List> response = restTemplate.getForEntity(buildRecommendationsUri(filter, id), List.class);
        List<?> body = response.getBody() == null ? List.of() : response.getBody();
        return convertScoredResults(body, filter.getResourceType());
    }

    @Override
    public <T> List<ScoredResult<T>> recommendByKey(FacetFilter filter, Map<String, String> keyValues) {
        ResponseEntity<List> response = restTemplate.getForEntity(buildKeyRecommendationsUri(filter, keyValues), List.class);
        List<?> body = response.getBody() == null ? List.of() : response.getBody();
        return convertScoredResults(body, filter.getResourceType());
    }

    @Override
    public <T> List<ScoredResult<T>> recommend(FacetFilter filter, T resource) {
        String url = buildResourceRecommendationsUri(filter);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> entity = new HttpEntity<>(resource, headers);
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.POST, entity, List.class);
        List<?> body = response.getBody() == null ? List.of() : response.getBody();
        return convertScoredResults(body, filter.getResourceType());
    }

    @Override
    public <T> T add(String resourceTypeName, T resource) {
        return add(resourceTypeName, resource, true);
    }

    @Override
    public <T> T add(String resourceTypeName, T resource, boolean validate) {
        if (!validate) {
            return addWithoutValidation(resourceTypeName, resource);
        }
        return exchangeBody(resourceTypeName, null, resource, HttpMethod.POST);
    }

    @Override
    public <T> T update(String resourceTypeName, T resource) {
        return update(resourceTypeName, resource, true);
    }

    @Override
    public <T> T update(String resourceTypeName, T resource, boolean validate) {
        return exchangeBody(resourceTypeName, null, resource, HttpMethod.PUT);
    }

    @Override
    public <T> boolean exists(String resourceTypeName, T resource) {
        SearchService.KeyValue[] keyValues = extractPrimaryKeys(resourceTypeName, resource);
        return searchResource(resourceTypeName, keyValues) != null;
    }

    @Override
    public <T> Map<String, String> getPrimaryKeyValues(String resourceTypeName, T resource) {
        SearchService.KeyValue[] keyValues = extractPrimaryKeys(resourceTypeName, resource);
        Map<String, String> result = new LinkedHashMap<>();
        for (SearchService.KeyValue keyValue : keyValues) {
            result.put(keyValue.getField(), keyValue.getValue());
        }
        return result;
    }

    @Override
    public <T> T delete(String resourceTypeName, String id) {
        ResponseEntity<Object> response = restTemplate.exchange(
                registryHost + "/records/" + resourceTypeName + "/" + id,
                HttpMethod.DELETE,
                null,
                Object.class
        );
        return convertBody(response.getBody(), resourceTypeName);
    }

    @Override
    public <T> T deleteByKey(String resourceTypeName, Map<String, String> keyValues) {
        ResponseEntity<Object> response = restTemplate.exchange(
                buildKeyUri(resourceTypeName, keyValues),
                HttpMethod.DELETE,
                null,
                Object.class
        );
        return convertBody(response.getBody(), resourceTypeName);
    }

    @Override
    public <T> T validate(String resourceTypeName, T resource) {
        return resource;
    }

    @Override
    public Class<?> getClassFromResourceType(String resourceTypeName) {
        try {
            ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
            if (resourceType == null || resourceType.getProperty("class") == null) {
                return Map.class;
            }
            return Class.forName(resourceType.getProperty("class"));
        } catch (ClassNotFoundException e) {
            return Map.class;
        }
    }

    @Override
    public Resource searchResource(String resourceTypeName, String id, boolean throwOnNull) {
        Resource resource = searchService.searchFields(resourceTypeName,
                new SearchService.KeyValue("resource_internal_id", id));
        if (resource == null && throwOnNull) {
            throw new ResourceNotFoundException(id, resourceTypeName);
        }
        return resource;
    }

    @Override
    public Resource searchResource(String resourceTypeName, SearchService.KeyValue... keyValues) {
        return searchService.searchFields(resourceTypeName, keyValues);
    }

    @Override
    public Resource searchResourceByKey(String resourceTypeName, Map<String, String> keyValues, boolean throwOnNull) {
        SearchService.KeyValue[] resolved = keyValues.entrySet().stream()
                .map(e -> new SearchService.KeyValue(e.getKey(), e.getValue()))
                .toArray(SearchService.KeyValue[]::new);
        Resource resource = searchService.searchFields(resourceTypeName, resolved);
        if (resource == null && throwOnNull) {
            throw new ResourceNotFoundException(joinKeyValues(keyValues), resourceTypeName);
        }
        return resource;
    }

    private <T> T addWithoutValidation(String resourceTypeName, T resource) {
        return exchangeBody(resourceTypeName, null, resource, HttpMethod.POST);
    }

    private <T> T exchangeBody(String resourceTypeName, String id, T resource, HttpMethod method) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> request = new HttpEntity<>(resource, headers);

        String path = id == null
                ? registryHost + "/records/" + resourceTypeName
                : registryHost + "/records/" + resourceTypeName + "/" + id;

        ResponseEntity<Object> response = restTemplate.exchange(path, method, request, Object.class);
        return convertBody(response.getBody(), resourceTypeName);
    }

    private String buildBrowseUri(FacetFilter filter, String mode, boolean highlighted) {
        String suffix = highlighted
                ? mode == null ? "/highlighted" : "/" + mode + "/highlighted"
                : mode == null ? "" : "/" + mode;
        String base = registryHost + "/records/" + filter.getResourceType() + suffix;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base)
                .queryParam("keyword", filter.getKeyword())
                .queryParam("from", filter.getFrom())
                .queryParam("quantity", filter.getQuantity())
                .queryParam("browseBy", filter.getBrowseBy());

        if (filter.getOrderBy() != null) {
            builder.queryParam("sort", filter.getOrderBy().keySet());
            builder.queryParam("order", filter.getOrderBy().values().stream()
                    .map(value -> ((Map<String, String>) value).get("order"))
                    .toList());
        }

        for (Map.Entry<String, Object> entry : filter.getFilter().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    builder.queryParam(entry.getKey(), item);
                }
            } else {
                builder.queryParam(entry.getKey(), value);
            }
        }

        return builder.toUriString();
    }

    private String buildRecommendationsUri(FacetFilter filter, String id) {
        String base = registryHost + "/records/" + filter.getResourceType() + "/" + id + "/recommendations";
        return buildRecommendationsBase(base, filter);
    }

    private String buildResourceRecommendationsUri(FacetFilter filter) {
        String base = registryHost + "/records/" + filter.getResourceType() + "/recommendations";
        return buildRecommendationsBase(base, filter);
    }

    private String buildRecommendationsBase(String base, FacetFilter filter) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base)
                .queryParam("keyword", filter.getKeyword())
                .queryParam("from", filter.getFrom())
                .queryParam("quantity", filter.getQuantity())
                .queryParam("browseBy", filter.getBrowseBy());

        for (Map.Entry<String, Object> entry : filter.getFilter().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    builder.queryParam(entry.getKey(), item);
                }
            } else {
                builder.queryParam(entry.getKey(), value);
            }
        }
        return builder.toUriString();
    }

    private String buildKeyUri(String resourceTypeName, Map<String, String> keyValues) {
        String base = registryHost + "/records/" + resourceTypeName + "/key";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base);
        keyValues.forEach(builder::queryParam);
        return builder.toUriString();
    }

    // Deliberately omits filter.getFilter(): GenericController's /key/recommendations route
    // treats every non-reserved query parameter as part of the composite primary key, so it
    // cannot also accept orthogonal facet-filter criteria in the same request.
    private String buildKeyRecommendationsUri(FacetFilter filter, Map<String, String> keyValues) {
        String base = registryHost + "/records/" + filter.getResourceType() + "/key/recommendations";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base)
                .queryParam("keyword", filter.getKeyword())
                .queryParam("from", filter.getFrom())
                .queryParam("quantity", filter.getQuantity())
                .queryParam("browseBy", filter.getBrowseBy());
        keyValues.forEach(builder::queryParam);
        return builder.toUriString();
    }

    private static String joinKeyValues(Map<String, String> keyValues) {
        return keyValues.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

    @SuppressWarnings("unchecked")
    private <T> List<ScoredResult<T>> convertScoredResults(List<?> items, String resourceTypeName) {
        return items.stream()
                .map(item -> {
                    ScoredResult<?> raw = objectMapper.convertValue(item, ScoredResult.class);
                    ScoredResult<T> sr = new ScoredResult<>();
                    sr.setScore(raw.getScore());
                    sr.setResult((T) convertValue(raw.getResult(), resourceTypeName));
                    return sr;
                })
                .toList();
    }

    private <T> Paging<T> convertPaging(Paging<?> paging, String resourceTypeName) {
        List<T> results = paging.getResults() == null ? List.of() : paging.getResults().stream()
                                                                    .map(item -> convertValue(item, resourceTypeName))
                                                                    .map(item -> (T) item)
                                                                    .toList();
        return new Paging<>(paging.getTotal(), paging.getFrom(), paging.getTo(), results, paging.getFacets());
    }

    private <T> Paging<HighlightedResult<T>> convertHighlightedPaging(Paging<?> paging, String resourceTypeName) {
        List<HighlightedResult<T>> results = new ArrayList<>();
        if (paging.getResults() != null) {
            for (Object item : paging.getResults()) {
                HighlightedResult<?> highlightedResult = objectMapper.convertValue(item, HighlightedResult.class);
                results.add(convertHighlightedResult(highlightedResult, resourceTypeName));
            }
        }
        return new Paging<>(paging.getTotal(), paging.getFrom(), paging.getTo(), results, paging.getFacets());
    }

    private <T> HighlightedResult<T> convertHighlightedResult(HighlightedResult<?> highlightedResult, String resourceTypeName) {
        Object converted = convertValue(highlightedResult.getResult(), resourceTypeName);
        return HighlightedResult.of(highlightedResult.getScore(), (T) converted, highlightedResult.getHighlights());
    }

    private <T> T convertBody(Object body, String resourceTypeName) {
        return (T) convertValue(body, resourceTypeName);
    }

    private Object convertValue(Object value, String resourceTypeName) {
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        Class<?> targetType = clazz == null ? Map.class : clazz;
        return objectMapper.convertValue(value, targetType);
    }

    private <T> T deserialize(Resource resource, String resourceTypeName) {
        return deserializePayload(resource.getPayload(), resourceTypeName);
    }

    private <T> T deserializePayload(String payload, String resourceTypeName) {
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        if (clazz == null || Map.class.equals(clazz)) {
            return (T) parsePayloadAsMap(payload);
        }
        try {
            return (T) objectMapper.readValue(payload, clazz);
        } catch (JacksonException e) {
            throw new ResourceException("Could not deserialize resource payload for " + resourceTypeName, HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    private Object parsePayloadAsMap(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (JacksonException e) {
            throw new ResourceException("Could not deserialize resource payload", HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    private <T> SearchService.KeyValue[] extractPrimaryKeys(String resourceTypeName, T resource) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null || resourceType.getIndexFields() == null) {
            throw new ResourceNotFoundException(resourceTypeName);
        }

        JsonNode root = objectMapper.valueToTree(resource);
        List<SearchService.KeyValue> keyValues = resourceType.getIndexFields().stream()
                .filter(IndexField::isPrimaryKey)
                .map(field -> new SearchService.KeyValue(field.getName(), extractJsonPathValue(root, field.getPath())))
                .filter(kv -> kv.getValue() != null)
                .toList();

        if (keyValues.isEmpty()) {
            throw new ResourceException("No primary key values found for " + resourceTypeName, HttpStatus.UNPROCESSABLE_CONTENT);
        }
        return keyValues.toArray(SearchService.KeyValue[]::new);
    }

    private String extractJsonPathValue(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        if (normalized.startsWith("$.")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("$")) {
            normalized = normalized.substring(1);
        }

        JsonNode current = root;
        for (String segment : normalized.split("\\.")) {
            if (segment.isBlank()) {
                continue;
            }
            if (segment.endsWith("[*]")) {
                String fieldName = segment.substring(0, segment.length() - 3);
                current = current.path(fieldName);
                if (!current.isArray() || current.isEmpty()) {
                    return null;
                }
                current = current.get(0);
            } else {
                current = current.path(segment);
            }
            if (current.isMissingNode() || current.isNull()) {
                return null;
            }
        }
        return current.isValueNode() ? current.asText() : null;
    }
}
