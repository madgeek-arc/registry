package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.service.IndexFieldService;
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

@Service("indexFieldService")
public class IndexFieldServiceImpl implements IndexFieldService {

    private static final Logger logger = LoggerFactory.getLogger(IndexFieldServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    @Override
    public List<IndexField> getIndexFields(String resourceTypeName) {
        RestTemplate restTemplate = new RestTemplate();


        ResponseEntity<List<IndexField>> response = restTemplate.exchange(
                registryHost + "/resourceType/index/" + resourceTypeName,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IndexField>>() {
                });
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return new ArrayList<>();
        }
    }
}