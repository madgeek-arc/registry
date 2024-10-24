package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.service.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service("versionService")
public class VersionServiceImpl implements VersionService {

    private static final Logger logger = LoggerFactory.getLogger(VersionServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    private List<Version> getListVersions(String url) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<Version>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Version>>() {
                });
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return new ArrayList<>();
        }
    }


    @Override
    public Version getVersion(String resource_id, String version) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Version> response = restTemplate.
                getForEntity(registryHost + "/version/whatever/" + resource_id + "/" + version, Version.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return null;
        }
    }

    @Override
    public List<Version> getVersionsByResource(String resource_id) {
        return getListVersions(registryHost + "/version/whatever/" + resource_id);
    }

    @Override
    public List<Version> getVersionsByResourceType(String resourceType_name) {
        return getListVersions(registryHost + "/version/" + resourceType_name);
    }

    @Override
    public List<Version> getAllVersions() {
        return getListVersions(registryHost + "/version");
    }

}