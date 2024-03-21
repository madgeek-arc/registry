package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
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
import java.util.List;
import java.util.Set;

@Service("resourceTypeService")
public class ResourceTypeServiceImpl implements ResourceTypeService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    private List<ResourceType> getListResourceTypes(String url) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Paging> response = restTemplate.getForEntity(url, Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody().getResults();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Schema getSchema(String id) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Schema> response = restTemplate.getForEntity(registryHost + "/schemaService/" + id, Schema.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return null;
        }
    }

    @Override
    public ResourceType getResourceType(String name) {
        RestTemplate restTemplate = new RestTemplate();
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
    public List<ResourceType> getAllResourceType(int from, int to) {
        return getListResourceTypes(registryHost + "/resourceType/?from=" + from + "&to=" + to);
    }

    @Override
    public ResourceType addResourceType(ResourceType resourceType) throws ServiceException {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);


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
        return null;
    }

    @Override
    public void deleteResourceType(String name) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(registryHost + "/resourceType/" + name);
    }
}
