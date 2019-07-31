package eu.openminted.registry.core.backup.restore;

import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.VersionDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.service.ResourceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
public class RestoreResourceWriterStep implements ItemWriter<Resource>, StepExecutionListener {

    private static final Logger logger = LogManager.getLogger(RestoreResourceWriterStep.class);

    private ResourceType resourceType;

    private ResourceService resourceService;

    private ResourceDao resourceDao;

    private VersionDao versionDao;

    private ElasticOperationsService elasticOperationsService;

    @Autowired
    public RestoreResourceWriterStep(ResourceService resourceService,
                                     ResourceDao resourceDao,
                                     VersionDao versionDao,
                                     ElasticOperationsService elasticOperationsService) {
        this.resourceService = resourceService;
        this.resourceDao = resourceDao;
        this.versionDao = versionDao;
        this.elasticOperationsService = elasticOperationsService;
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
    public void write(List<? extends Resource> items) {
        try {
            List<Resource> resources = new ArrayList<>();
            logger.debug("Adding resources - " + items.size());
            for (Resource resource : items) {
                Resource addedResource = resource;
                if (resource.getId() == null) {
                    addedResource = resourceService.addResource(resource);
                } else {
                    // we are using the DAO service in order to keep the previous ID of the resource
                    resourceDao.addResource(resource);
                }

                resource.getVersions().forEach(v -> versionDao.addVersion(v));
                logger.debug("Restoring " + resourceType.getName() + " with id " + addedResource.getId());
                resources.add(addedResource);

            }
            elasticOperationsService.addBulk(resources);
        } catch (Exception e) {
            logger.info(e.getMessage(),e);
        }
    }
}
