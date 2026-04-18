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

package gr.uoa.di.madgik.registry.backup.restore;

import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.dao.VersionDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
public class RestoreResourceWriterStep implements ItemWriter<Resource>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(RestoreResourceWriterStep.class);

    private ResourceType resourceType;

    private final ResourceService resourceService;
    private final ResourceDao resourceDao;
    private final VersionDao versionDao;
    private final IndexOperationsService indexOperationsService;
    private final ResourceTypeService resourceTypeService;
    private final ResourceChunkIndexService resourceChunkIndexService;

    public RestoreResourceWriterStep(ResourceService resourceService,
                                     ResourceDao resourceDao,
                                     VersionDao versionDao,
                                     IndexOperationsService indexOperationsService,
                                     ResourceTypeService resourceTypeService,
                                     ResourceChunkIndexService resourceChunkIndexService) {
        this.resourceService = resourceService;
        this.resourceDao = resourceDao;
        this.versionDao = versionDao;
        this.indexOperationsService = indexOperationsService;
        this.resourceTypeService = resourceTypeService;
        this.resourceChunkIndexService = resourceChunkIndexService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        resourceType = resourceTypeService.getResourceType(executionContext.getString("resourceTypeName"));
        if (resourceType == null) {
            stepExecution.addFailureException(new ServiceException("Resource Type not provided"));
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return ExitStatus.COMPLETED;
    }


    @Override
    public void write(Chunk<? extends Resource> chunk) throws Exception {
        try {
            List<Resource> resources = new ArrayList<>();
            logger.debug("Adding resources - " + chunk.size());
            for (Resource resource : chunk) {
                Resource addedResource = resource;
                if (resource.getId() == null) {
                    addedResource = resourceService.addResource(resource);
                } else {
                    // we are using the DAO service in order to keep the previous ID of the resource
                    resource = resourceDao.addResource(resource);
                }

                resource.getVersions().forEach(versionDao::addVersion);
                logger.debug("Restoring {} with id {}", resourceType.getName(), addedResource.getId());

                // create resource_chunks -- not to be confused with previous chunk ^ of spring batch
                resourceChunkIndexService.reindex(resource);

                resources.add(addedResource);
            }
            indexOperationsService.addBulk(resources);
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }
    }
}
