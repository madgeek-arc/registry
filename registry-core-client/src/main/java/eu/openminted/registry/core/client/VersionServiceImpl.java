package eu.openminted.registry.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

@Service("versionService")
public class VersionServiceImpl implements VersionService {

    private static Logger logger = LogManager.getLogger(VersionServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    private List<Version> getListVersions(String url){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<Version>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Version>>(){});
        if(response.getStatusCode().is2xxSuccessful()){
            return response.getBody();
        }else{
            return new ArrayList<>();
        }
    }


    @Override
    public Version getVersion(String resource_id, String version) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Version> response = restTemplate.
                getForEntity(registryHost+"/version/whatever/"+resource_id+"/"+version, Version.class);
        if(response.getStatusCode().is2xxSuccessful()){
            return response.getBody();
        }else{
            return null;
        }
    }

    @Override
    public List<Version> getVersionsByResource(String resource_id) {
        return getListVersions(registryHost+"/version/whatever/"+resource_id);
    }

    @Override
    public List<Version> getVersionsByResourceType(String resourceType_name) {
        return getListVersions(registryHost+"/version/"+resourceType_name);
    }

    @Override
    public List<Version> getAllVersions() {
        return getListVersions(registryHost+"/version");
    }

    @Override
    public void addVersion(Version version) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);


        HttpEntity<Version> request = new HttpEntity<>(version,headers);
        ResponseEntity<Version> response = restTemplate
                .exchange(registryHost+"/version", HttpMethod.POST, request, Version.class);

    }
}