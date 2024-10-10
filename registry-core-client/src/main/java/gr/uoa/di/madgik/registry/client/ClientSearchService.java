package gr.uoa.di.madgik.registry.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(registryHost + "/search/cql/" + resourceType + "/" + query + "/");
        MultiValueMap<String, Object> filters = new LinkedMultiValueMap<>();
        filters.add("from", from);
        filters.add("quantity", quantity);
        filters.add("sortBy", sortByField);
        filters.add("sortOrder", sortOrder);
        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class, createParams(filters));
        if (response.getStatusCode().is2xxSuccessful()) {
            return convert(response.getBody());
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(registryHost + "/search/cql/" + resourceType + "/" + query + "/", Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return convert(response.getBody());
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) throws ServiceException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(registryHost + "/search/" + filter.getResourceType());
        ResponseEntity<Paging> response;
        try {
            response = restTemplate.getForEntity(builder.toUriString(), Paging.class, createParams(filter.getFilter()));
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
    public Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(registryHost + "/search/" + resourceType);
        Map<String, Object> filters = new HashMap<>();
        filters.put("keyword", keyword);

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class, createParams(filters));
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

    private HttpEntity<Map<String, Object>> createParams(Map<String, Object> filters) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(filters, headers);
    }

    private HttpEntity<MultiValueMap<String, Object>> createParams(MultiValueMap<String, Object> filters) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(filters, headers);
    }
}