/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.domain.BatchResult;
import gr.uoa.di.madgik.registry.service.RestoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service("restoreService")
public class RestoreServiceImpl implements RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(RestoreServiceImpl.class);

    private final RestTemplate restTemplate;
    private final String registryHost;

    public RestoreServiceImpl(RestTemplate restTemplate,
                              @Value("${registry.base}") String registryHost) {
        this.restTemplate = restTemplate;
        this.registryHost = registryHost;
    }

    public Map<String, BatchResult> restoreDataFromZip(MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("datafile", file);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, BatchResult>> response = restTemplate.exchange(
                registryHost + "/restore",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );
        logger.info("Restore completed, response status: {}", response.getStatusCode());
        return response.getBody();
    }
}