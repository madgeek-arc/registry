/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.service.IndexedFieldService;
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
                registryHost + "/resources/indexed/" + resourceId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IndexedField>>() {
                });
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return new ArrayList<>();
        }
    }

}