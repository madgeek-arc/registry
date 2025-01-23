package gr.uoa.di.madgik.registry.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("resourceTypeService")
public class ResourceTypeServiceImpl implements ResourceTypeService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeServiceImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${registry.base}")
    private String registryHost;

    public ResourceTypeServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private List<ResourceType> getListResourceTypes(String url) {
        ResponseEntity<Paging> response = restTemplate.getForEntity(url, Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return (List<ResourceType>) response.getBody().getResults()
                    .stream()
                    .map(rt -> objectMapper.convertValue(rt, ResourceType.class))
                    .toList();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Schema getSchema(String id) {
        ResponseEntity<Schema> response = restTemplate.getForEntity(registryHost + "/schemaService/" + id, Schema.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return null;
        }
    }

    @Override
    public ResourceType getResourceType(String name) {
        ResponseEntity<ResourceType> response = restTemplate.getForEntity(registryHost + "/resourceType/" + name, ResourceType.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return null;
        }
    }

    @Override
    public List<ResourceType> getAllResourceType() {
        return getListResourceTypes(registryHost + "/resourceType/");
    }

    @Override
    public List<ResourceType> getAllResourceTypeByAlias(String alias) {
        List<ResourceType> resourceTypes = new ArrayList<>();
        ResponseEntity<List> response = restTemplate.getForEntity(registryHost + "/resourceType/" + alias, List.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            resourceTypes.addAll(response.getBody()
                    .stream()
                    .map(i -> objectMapper.convertValue(i, ResourceType.class))
                    .toList());
        }
        return resourceTypes;
    }

    @Override
    public List<ResourceType> getAllResourceType(int from, int to) {
        return getListResourceTypes(registryHost + "/resourceType/?from=" + from + "&to=" + to);
    }

    @Override
    public ResourceType addResourceType(ResourceType resourceType) throws ServiceException {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);


        HttpEntity<ResourceType> request = new HttpEntity<>(resourceType, headers);
        ResponseEntity<ResourceType> response = restTemplate
                .exchange(registryHost + "/resourceType", HttpMethod.POST, request, ResourceType.class);

        if (response.getStatusCode().is2xxSuccessful())
            return response.getBody();
        else
            return null;
    }

    @Override
    public Set<IndexField> getResourceTypeIndexFields(String name) {
        ResourceType resourceType = getResourceType(name);
        Set<IndexField> indexFields = new HashSet<>();
        ResponseEntity<Set> response = restTemplate.getForEntity(registryHost + "/resourceType/index/" + name, Set.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            indexFields.addAll((Set<IndexField>) response.getBody()
                    .stream()
                    .map(i -> objectMapper.convertValue(i, IndexField.class))
                    .map(e -> { ((IndexField) e).setResourceType(resourceType); return e;})
                    .collect(Collectors.toSet())
            );
            return indexFields;
        }
        return null;
    }

    @Override
    public void deleteResourceType(String name) {
        restTemplate.delete(registryHost + "/resourceType/" + name);
    }
}
