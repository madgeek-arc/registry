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

import gr.uoa.di.madgik.registry.service.DumpService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

@Service
public class DumpServiceImpl implements DumpService {

    private static final Logger logger = LoggerFactory.getLogger(DumpServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    @Override
    public File dump(boolean isRaw, boolean schemaless, String[] resourceTypes, boolean wantVersion) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<byte[]> response = restTemplate.getForEntity(registryHost + "/dump/?schema=" + schemaless + "&raw=" + isRaw + ((resourceTypes.length == 0) ? "" : "&resourceTypes=" + String.join(",", resourceTypes)), byte[].class);
        if (response.getStatusCode().is2xxSuccessful()) {
            FileOutputStream output = null;
            try {
                File file = File.createTempFile("dump-" + (new Date().getTime()), ".zip");
                output = new FileOutputStream(file);
                IOUtils.write(response.getBody(), output);
                return file;
            } catch (IOException e) {
                logger.debug("Could not get file from REST", e);
            }
        }
        return null;
    }
}