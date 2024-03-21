package eu.openminted.registry.core.client;

import eu.openminted.registry.core.service.DumpService;
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

@Service("dumpService")
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