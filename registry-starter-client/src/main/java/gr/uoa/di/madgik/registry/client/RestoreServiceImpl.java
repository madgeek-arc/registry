package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.domain.BatchResult;
import gr.uoa.di.madgik.registry.service.RestoreService;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service("restoreService")
public class RestoreServiceImpl implements RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(RestoreServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    public Map<String, BatchResult> restoreDataFromZip(MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("datafile", file);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(registryHost + "/restore", requestEntity, String.class);

        logger.info(response.getBody());

        return null;
    }
}