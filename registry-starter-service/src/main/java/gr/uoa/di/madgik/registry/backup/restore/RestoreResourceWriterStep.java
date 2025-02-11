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

import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.dao.VersionDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
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

    @Autowired
    public RestoreResourceWriterStep(ResourceService resourceService,
                                     ResourceDao resourceDao,
                                     VersionDao versionDao,
                                     IndexOperationsService indexOperationsService) {
        this.resourceService = resourceService;
        this.resourceDao = resourceDao;
        this.versionDao = versionDao;
        this.indexOperationsService = indexOperationsService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        resourceType = (ResourceType) executionContext.get("resourceType");
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

                resource.getVersions().forEach(v -> versionDao.addVersion(v));
                logger.debug("Restoring " + resourceType.getName() + " with id " + addedResource.getId());
                resources.add(addedResource);

            }
            indexOperationsService.addBulk(resources);
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }
    }
}
