package eu.openminted.registry.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ServiceException;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service("resourceService")
public class ResourceServiceImpl implements ResourceService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    private List<Resource> getListResources(String url) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Paging<Resource> paging = objectMapper.readValue(response.getBody(), Paging.class);
                return paging.getResults();
            } catch (IOException e) {
                logger.debug("Failed to deserialize response to Resource object", e);
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Resource getResource(String id) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(registryHost + "/resources/whatever/" + id, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(response.getBody(), Resource.class);
            } catch (IOException e) {
                logger.debug("Failed to deserialize response to Resource object", e);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public List<Resource> getResource(ResourceType resourceType) {
        return getListResources(registryHost + "/resources/" + resourceType.getName());
    }

    @Override
    public void getResourceStream(Consumer<Resource> consumer) {

    }

    @Override
    public List<Resource> getResource(ResourceType resourceType, int from, int to) {
        return getListResources(registryHost + "/resources/" + resourceType.getName() + "/?from=" + from + "&to=" + to);
    }

    @Override
    public List<Resource> getResource(int from, int to) {
        return getListResources(registryHost + "/resources/" + "?from=" + from + "&to=" + to);
    }

    @Override
    public List<Resource> getResource() {
        return getListResources(registryHost + "/resources/");
    }

    @Override
    public Resource addResource(Resource resource) throws ServiceException {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);


        HttpEntity<Resource> request = new HttpEntity<>(resource, headers);
        ResponseEntity<Resource> response = restTemplate
                .exchange(registryHost + "/resources", HttpMethod.POST, request, Resource.class);

        if (response.getStatusCode().is2xxSuccessful())
            return response.getBody();
        else
            return null;
    }

    @Override
    public Resource updateResource(Resource resource) throws ServiceException {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);


        HttpEntity<Resource> request = new HttpEntity<>(resource, headers);

        ResponseEntity<Resource> response = restTemplate
                .exchange(registryHost + "/resources", HttpMethod.PUT, request, Resource.class);

        if (response.getStatusCode().is2xxSuccessful())
            return response.getBody();
        else
            return null;
    }

    @Override
    public Resource changeResourceType(Resource resource, ResourceType resourceType) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);


        HttpEntity<Resource> request = new HttpEntity<>(resource, headers);

        ResponseEntity<Resource> response = restTemplate
                .exchange(registryHost + "/resource/" + resource.getId() + "/" + resourceType.getName(), HttpMethod.POST, request, Resource.class);

        if (response.getStatusCode().is2xxSuccessful())
            return response.getBody();
        else
            return null;
    }

    @Override
    public void deleteResource(String id) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(registryHost + "/resources/" + id);
    }

}
