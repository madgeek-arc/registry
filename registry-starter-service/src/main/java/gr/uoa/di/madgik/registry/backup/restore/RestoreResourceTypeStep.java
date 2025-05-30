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

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Component
@StepScope
public class RestoreResourceTypeStep implements Tasklet, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(RestoreResourceTypeStep.class);

    private static final String schemaName = "schema.json";

    private ResourceTypeService resourceTypeService;

    private File resourceTypeDirFile;

    private Optional<ResourceType> existingResourceType;

    private File schemaFile;

    private Boolean resourceTypeExists;

    RestoreResourceTypeStep(ResourceTypeService resourceTypeService) {
        this.resourceTypeService = resourceTypeService;
        resourceTypeExists = false;
    }

    private static ResourceType readResourceType(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(FileUtils
                        .readFileToString(file)
                        .replaceAll("^\t$", "")
                        .replaceAll("^\n$", "")
                , ResourceType.class);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String resourceTypeDir = stepExecution.getJobExecution().getJobParameters().getString("resourceTypeDir");
        resourceTypeDirFile = new File(resourceTypeDir);
        schemaFile = new File(resourceTypeDir, schemaName);
        existingResourceType = Optional.ofNullable(resourceTypeService.getResourceType(resourceTypeDirFile.getName()));

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ResourceType resourceType = existingResourceType.orElse(null);
        if (resourceType == null) {
            return ExitStatus.FAILED;
        }
        Optional<File[]> resources = Optional.ofNullable(
                resourceTypeDirFile.listFiles(f -> !f.getName().equalsIgnoreCase("schema.json") && !f.isDirectory())
        );
        stepExecution.getJobExecution().getExecutionContext().put("resources", resources.orElse(new File[]{}));
        stepExecution.getJobExecution().getExecutionContext().put("resourceType", resourceType);
        if (schemaFile.exists())
            return ExitStatus.COMPLETED;
        else
            return ExitStatus.NOOP;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (schemaFile.exists()) {
            ResourceType resourceType = readResourceType(schemaFile);
            existingResourceType.ifPresent(r -> {
                logger.info("Resource type is present, deleting it..");
                resourceTypeService.deleteResourceType(r.getName());
            });
            logger.info("Adding resource type " + resourceType.getName());
            if ("not_set".equals(resourceType.getSchemaUrl())) {
                resourceType.setSchemaUrl(null);
            }
            if (!StringUtils.isEmpty(resourceType.getSchemaUrl()))
                resourceType.setSchema(null);
            existingResourceType = Optional.of(resourceTypeService.addResourceType(resourceType));
        }
        String name = existingResourceType.orElseThrow(() -> new ServiceException("Resource Type not provided")).getName();
        logger.info("Resource type " + name + " added");
        return RepeatStatus.FINISHED;
    }

}
