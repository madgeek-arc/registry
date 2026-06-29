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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service("searchService")
public class ClientSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(ClientSearchService.class);


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String registryHost;

    public ClientSearchService(RestTemplate restTemplate,
                               ObjectMapper objectMapper,
                               @Value("${registry.base}") String registryHost) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.registryHost = registryHost;
    }

    @SuppressWarnings("unchecked")
    private Paging<Resource> convert(Paging paging) {
        if (paging != null && !paging.getResults().isEmpty()) {
            paging.setResults(
                    paging.getResults()
                            .stream()
                            .map(i -> objectMapper.convertValue(i, Resource.class))
                            .toList()
            );
        }
        return paging;
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType, int quantity, int from, String sortByField, String sortOrder) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(registryHost + "/search/cql/" + resourceType)
                .queryParam("query", query)
                .queryParam("from", from)
                .queryParam("quantity", quantity)
                .queryParam("sort", sortByField)
                .queryParam("order", sortOrder);

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return convert(response.getBody());
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(registryHost + "/search/cql/" + resourceType)
                .queryParam("query", query);

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return convert(response.getBody());
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) throws ServiceException {
        return executeSearch(filter, null);
    }

    @Override
    public Paging<Resource> semanticSearch(FacetFilter filter) throws ServiceException {
        return executeSearch(filter, "semantic");
    }

    @Override
    public Paging<Resource> hybridSearch(FacetFilter filter) throws ServiceException {
        return executeSearch(filter, "hybrid");
    }

    @Override
    public Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) throws ServiceException {
        return executeHighlightedSearch(filter, null);
    }

    @Override
    public Paging<HighlightedResult<Resource>> hybridSearchWithHighlights(FacetFilter filter) throws ServiceException {
        return executeHighlightedSearch(filter, "hybrid");
    }

    private Paging<Resource> executeSearch(FacetFilter filter, String mode) {
        String path = mode == null
                ? registryHost + "/search/" + filter.getResourceType()
                : registryHost + "/search/" + filter.getResourceType() + "/" + mode;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(path)
                .queryParam("keyword", filter.getKeyword())
                .queryParam("from", filter.getFrom())
                .queryParam("quantity", filter.getQuantity())
                .queryParam("browseBy", filter.getBrowseBy());
        if (filter.getOrderBy() != null) {
            builder.queryParam("sort", filter.getOrderBy().keySet());
            builder.queryParam("order", filter.getOrderBy().values()
                    .stream()
                    .map(value -> ((Map<String, String>)value).get("order"))
                    .toList());
        }

        for (Map.Entry<String, Object> entry : filter.getFilter().entrySet()) {
            Object value = entry.getValue();
            if (Collection.class.isAssignableFrom(value.getClass())) {
                for (String val : ((List<String>) value)) {
                    builder.queryParam(entry.getKey(), val);
                }
            } else {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }

        ResponseEntity<Paging> response;
        try {
            response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return convert(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() != 404) {
                throw e;
            }
        }
        return new Paging<>();
    }

    @Override
    public List<ScoredResult<Resource>> recommend(FacetFilter filter, KeyValue idValue) throws ServiceException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<ScoredResult<Resource>> recommend(FacetFilter filter, Resource queryResource) throws ServiceException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(registryHost + "/search/" + resourceType)
                .queryParam("keyword", keyword);

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return convert(response.getBody());
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException {
        List<String> filters = new ArrayList<>();
        for (KeyValue keyValue : fields)
            filters.add(keyValue.getField() + "=\"" + keyValue.getValue() + "\"");
        List<Resource> resources = cqlQuery(String.join(" AND ", filters), resourceType).getResults();
        if (!resources.isEmpty())
            return objectMapper.convertValue(resources.get(0), Resource.class);
        else
            return null;
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private Paging<HighlightedResult<Resource>> executeHighlightedSearch(FacetFilter filter, String mode) {
        String path = mode == null
                ? registryHost + "/search/" + filter.getResourceType() + "/highlighted"
                : registryHost + "/search/" + filter.getResourceType() + "/" + mode + "/highlighted";
        UriComponentsBuilder builder = buildSearchUri(path, filter);

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
        Paging<?> paging = response.getBody() == null ? new Paging<>() : response.getBody();
        return convertHighlightedPaging(paging);
    }

    private UriComponentsBuilder buildSearchUri(String path, FacetFilter filter) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(path)
                .queryParam("keyword", filter.getKeyword())
                .queryParam("from", filter.getFrom())
                .queryParam("quantity", filter.getQuantity())
                .queryParam("browseBy", filter.getBrowseBy());
        if (filter.getOrderBy() != null) {
            builder.queryParam("sort", filter.getOrderBy().keySet());
            builder.queryParam("order", filter.getOrderBy().values()
                    .stream()
                    .map(value -> ((Map<String, String>) value).get("order"))
                    .toList());
        }

        for (Map.Entry<String, Object> entry : filter.getFilter().entrySet()) {
            Object value = entry.getValue();
            if (Collection.class.isAssignableFrom(value.getClass())) {
                for (String val : ((List<String>) value)) {
                    builder.queryParam(entry.getKey(), val);
                }
            } else {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    @SuppressWarnings("unchecked")
    private Paging<HighlightedResult<Resource>> convertHighlightedPaging(Paging<?> paging) {
        Paging<HighlightedResult<Resource>> converted = new Paging<>();
        converted.setTotal(paging.getTotal());
        converted.setFrom(paging.getFrom());
        converted.setTo(paging.getTo());
        converted.setFacets(paging.getFacets());
        converted.setResults(paging.getResults().stream()
                .map(item -> objectMapper.convertValue(item, HighlightedResult.class))
                .map(raw -> {
                    HighlightedResult<Resource> result = new HighlightedResult<>();
                    result.setScore(raw.getScore());
                    result.setHighlights(raw.getHighlights());
                    result.setResult(objectMapper.convertValue(Objects.requireNonNull(raw.getResult()), Resource.class));
                    return result;
                })
                .toList());
        return converted;
    }
}
