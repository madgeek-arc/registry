package eu.openminted.registry.core.client;

import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.IndexedFieldService;
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

@Service("indexedFieldService")
public class IndexedFieldServiceImpl implements IndexedFieldService {

    private static final Logger logger = LoggerFactory.getLogger(IndexedFieldServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    @Override
    public List<IndexedField> getIndexedFields(String resourceId) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<List<IndexedField>> response = restTemplate.exchange(
                registryHost+"/resources/indexed/"+resourceId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IndexedField>>(){});
        if(response.getStatusCode().is2xxSuccessful()){
                return response.getBody();
        }else{
            return new ArrayList<>();
        }
    }

}