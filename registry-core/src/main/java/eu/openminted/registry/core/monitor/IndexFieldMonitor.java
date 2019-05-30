package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.dao.ViewDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.service.ResourceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by antleb on 5/26/16.
 */
@Aspect
@Component
public class IndexFieldMonitor {

    private static Logger logger = LogManager.getLogger(IndexFieldMonitor.class);

    @Autowired
    private ViewDao viewDao;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ResourceService resourceService;

    @AfterReturning(pointcut = "execution (* eu.openminted.registry.core.service.IndexFieldService.delete(..)) && args(..,resourceTypeName" +
            ")")
    public void indexFieldRemoved(String resourceTypeName) throws Throwable {
        logger.info("Recreating "+resourceTypeName + " view");
        ResourceType resourceType = resourceTypeDao.getResourceType(resourceTypeName);
        viewDao.deleteView(resourceType.getName());
        viewDao.createView(resourceType);

        for(Resource resource: resourceDao.getResource(resourceType)){
            resourceService.updateResource(resource);
        }
    }

    @AfterReturning(pointcut = "execution (* eu.openminted.registry.core.service.IndexFieldService.add(..))", returning = "indexField")
    public void indexFieldAdded(IndexField indexField) {
        logger.info("Recreating "+indexField.getResourceType().getName() + " view");
        viewDao.deleteView(indexField.getResourceType().getName());
        viewDao.createView(indexField.getResourceType());

        for(Resource resource: resourceDao.getResource(indexField.getResourceType())){
            resourceService.updateResource(resource);
        }

    }
}