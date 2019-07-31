package eu.openminted.registry.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.service.DumpService;
import eu.openminted.registry.core.service.VersionService;
import org.apache.commons.io.IOUtils;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("dumpService")
public class DumpServiceImpl implements DumpService {

    private static Logger logger = LogManager.getLogger(DumpServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;

    @Override
    public File dump(boolean isRaw, boolean schemaless, String[] resourceTypes, boolean wantVersion) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<byte[]> response = restTemplate.getForEntity(registryHost + "/dump/?schema="+schemaless+"&raw="+isRaw+ ((resourceTypes.length==0) ? "" : "&resourceTypes="+String.join(",",resourceTypes)), byte[].class);
        if(response.getStatusCode().is2xxSuccessful()){
            FileOutputStream output = null;
            try {
                File file = File.createTempFile("dump-"+(new Date().getTime()),".zip");
                output = new FileOutputStream(file);
                IOUtils.write(response.getBody(), output);
                return file;
            } catch (IOException e) {
                logger.debug("Could not get file from REST",e);
            }
        }
        return null;
    }
}