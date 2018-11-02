package eu.openminted.registry.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.Set;
import java.util.function.Consumer;

@Service("resourceTypeService")
public class ResourceTypeServiceImpl implements ResourceTypeService {

    private static Logger logger = LogManager.getLogger(ResourceTypeServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    private List<ResourceType> getListResourceTypes(String url){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Paging> response = restTemplate.getForEntity(url, Paging.class);
        if(response.getStatusCode().is2xxSuccessful()){
            return response.getBody().getResults();
        }else{
            return new ArrayList<>();
        }
    }

    @Override
    public Schema getSchema(String id) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Schema> response = restTemplate.getForEntity(registryHost + "/schemaService/" + id, Schema.class);
        if(response.getStatusCode().is2xxSuccessful()){
            return response.getBody();
        }else{
            return null;
        }
    }

    @Override
    public ResourceType getResourceType(String name) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ResourceType> response = restTemplate.getForEntity(registryHost + "/resourceType/" + name, ResourceType.class);
        if(response.getStatusCode().is2xxSuccessful()){
            return response.getBody();
        }else{
            return null;
        }
    }

    @Override
    public List<ResourceType> getAllResourceType() {
        return getListResourceTypes(registryHost+"/resourceType/");
    }

    @Override
    public List<ResourceType> getAllResourceType(int from, int to) {
        return getListResourceTypes(registryHost+"/resourceType/?from="+from+"&to="+to);
    }

    @Override
    public ResourceType addResourceType(ResourceType resourceType) throws ServiceException {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);


        HttpEntity<ResourceType> request = new HttpEntity<>(resourceType,headers);
        ResponseEntity<ResourceType> response = restTemplate
                .exchange(registryHost+"/resourceType", HttpMethod.POST, request, ResourceType.class);

        if(response.getStatusCode().is2xxSuccessful())
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
        restTemplate.delete(registryHost+"/resourceType/"+name);
    }
}
