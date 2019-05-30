package eu.openminted.registry.core.client;

import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.service.IndexFieldService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static Logger logger = LogManager.getLogger(IndexFieldServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    @Override
    public List<IndexField> getIndexFields(String resourceTypeName) {
        RestTemplate restTemplate = new RestTemplate();


        ResponseEntity<List<IndexField>> response = restTemplate.exchange(
                registryHost+"/resourceType/index/"+resourceTypeName,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IndexField>>(){});
        if(response.getStatusCode().is2xxSuccessful()){
            return response.getBody();
        }else{
            return new ArrayList<>();
        }
    }

    @Override
    public IndexField getIndexField(String indexFieldName) {
        return null;
    }

    @Override
    public IndexField add(IndexField indexField) {
        return null;
    }

    @Override
    public void delete(String indexField, String resourceTypeName) {

    }

}