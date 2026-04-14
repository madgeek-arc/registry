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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
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
    public <T> Paging<T> getResults(FacetFilter filter) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(buildBrowseUri(filter, false), Paging.class);
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
        ResponseEntity<Paging> response = restTemplate.getForEntity(buildBrowseUri(filter, true), Paging.class);
        Paging<?> paging = response.getBody() == null ? new Paging<>() : response.getBody();
        return convertHighlightedPaging(paging, Objects.requireNonNull(filter.getResourceType()));
    }

    @Override
    public <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented by registry-starter-client");
    }

    @Override
    public <T> List<T> recommend(FacetFilter filter, String id) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                buildRecommendationsUri(filter, id),
                List.class
        );
        List<?> body = response.getBody() == null ? List.of() : response.getBody();
        return body.stream()
                .map(item -> convertValue(item, filter.getResourceType()))
                .map(item -> (T) item)
                .toList();
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
        if (!validate) {
            return exchangeBody(resourceTypeName, resolvePrimaryId(resourceTypeName, resource), resource, HttpMethod.PUT);
        }
        return exchangeBody(resourceTypeName, resolvePrimaryId(resourceTypeName, resource), resource, HttpMethod.PUT);
    }

    @Override
    public <T> boolean exists(String resourceTypeName, T resource) {
        SearchService.KeyValue[] keyValues = extractPrimaryKeys(resourceTypeName, resource);
        return searchResource(resourceTypeName, keyValues) != null;
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

    private String buildBrowseUri(FacetFilter filter, boolean highlighted) {
        String base = registryHost + "/records/" + filter.getResourceType() + (highlighted ? "/highlighted" : "");
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
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        if (clazz == null || Map.class.equals(clazz)) {
            return (T) parsePayloadAsMap(resource.getPayload());
        }
        try {
            return (T) objectMapper.readValue(resource.getPayload(), clazz);
        } catch (IOException e) {
            throw new ResourceException("Could not deserialize resource payload for " + resourceTypeName, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Object parsePayloadAsMap(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (IOException e) {
            throw new ResourceException("Could not deserialize resource payload", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private <T> String resolvePrimaryId(String resourceTypeName, T resource) {
        SearchService.KeyValue[] primaryKeys = extractPrimaryKeys(resourceTypeName, resource);
        if (primaryKeys.length == 0) {
            throw new ResourceException("No primary key fields found for " + resourceTypeName, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return primaryKeys[0].getValue();
    }

    private <T> SearchService.KeyValue[] extractPrimaryKeys(String resourceTypeName, T resource) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null || resourceType.getIndexFields() == null) {
            throw new ResourceException("ResourceType " + resourceTypeName + " not found", HttpStatus.NOT_FOUND);
        }

        JsonNode root = objectMapper.valueToTree(resource);
        List<SearchService.KeyValue> keyValues = resourceType.getIndexFields().stream()
                .filter(IndexField::isPrimaryKey)
                .map(field -> new SearchService.KeyValue(field.getName(), extractJsonPathValue(root, field.getPath())))
                .filter(kv -> kv.getValue() != null)
                .toList();

        if (keyValues.isEmpty()) {
            throw new ResourceException("No primary key values found for " + resourceTypeName, HttpStatus.UNPROCESSABLE_ENTITY);
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
