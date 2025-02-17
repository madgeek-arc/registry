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

package gr.uoa.di.madgik.registry.backup.restore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.index.IndexMapper;
import gr.uoa.di.madgik.registry.index.IndexMapperFactory;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@StepScope
public class RestoreResourceReaderStep implements ItemReader<Resource>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(RestoreResourceReaderStep.class);

    private ResourceType resourceType;

    private Queue<File> resources;

    private final ResourceDao resourceDao;

    private final ObjectMapper mapper;

    private final IndexMapperFactory indexMapperFactory;

    private IndexMapper indexMapper;

    @Autowired
    public RestoreResourceReaderStep(ResourceDao resourceDao, IndexMapperFactory indexMapperFactory) {
        this.resourceDao = resourceDao;
        this.indexMapperFactory = indexMapperFactory;
        this.mapper = new ObjectMapper();
        this.mapper.configure(MapperFeature.USE_ANNOTATIONS, true);
        this.mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        resourceType = (ResourceType) executionContext.get("resourceType");
        File[] files = (File[]) executionContext.get("resources");
        resources = new ConcurrentLinkedQueue<>(Arrays.asList(files));

        try {
            indexMapper = indexMapperFactory.createIndexMapper(resourceType);
        } catch (Exception e) {
            stepExecution.addFailureException(e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return ExitStatus.COMPLETED;
    }

    @Override
    public Resource read() throws Exception {
        File file = resources.poll();
        if (file == null)
            return null;
        logger.debug("Reading " + file.getName());
        String switchEnc = String.format("%s->%s", FilenameUtils.getExtension(file.getName()), resourceType.getPayloadType());
        Resource resource = new Resource();
        switch (switchEnc.toLowerCase()) {
            case "json->xml":
                try {
                    resource = mapper.readValue(file, Resource.class);
                } catch (Exception e) {
                    logger.info("Exception", e);
                }

                break;
            case "json->json":
                JsonNode node = mapper.readTree(file);
                if (!node.path("payloadFormat").isMissingNode()) {
                    resource = mapper.treeToValue(node, Resource.class);
                    break;
                }
            case "xml->xml":
                resource.setPayload(FileUtils.readFileToString(file));
                break;
            case "xml->json":
            default:
                throw new UnexpectedInputException("Not A supported file transformation");

        }
        if (resourceDao.getResource(resource.getId()) != null) {
            logger.debug("Skipping resource " + resource.getId() + " already exists");
            throw new ServiceException("Existing resource");
        }
        final Resource lambdaResource = resource;
        resource.setResourceType(resourceType);
        resource.setPayloadFormat(resourceType.getPayloadType());
        resource.setIndexedFields(indexMapper.getValues(resource.getPayload(), resourceType));
        resource.getIndexedFields().forEach(x -> x.setResource(lambdaResource));
        resource.setVersions(readResourceVersions(file, resource));
        resource.getVersions().forEach(x -> x.setResource(lambdaResource));
        resource.setCreationDate(lambdaResource.getCreationDate());
        return resource;
    }

    private List<Version> readResourceVersions(File resourceDir, Resource resource) throws IOException {
        List<Version> ret = new ArrayList<>();
        String id = FilenameUtils.removeExtension(resourceDir.getName());
        File version = new File(FilenameUtils.removeExtension(resourceDir.getAbsolutePath()) + "-version");
        if (version.exists() && version.isDirectory()) {
            logger.debug(id + " " + version);
            Optional<File[]> files = Optional.ofNullable(version.listFiles());
            for (File f : files.orElse(new File[]{})) {
                Version v = mapper.readValue(f, Version.class);
                v.setResource(resource);
                v.setResourceType(resourceType);
                v.setVersion(UUID.randomUUID().toString());
                ret.add(v);
            }
        }
        return ret;
    }
}
