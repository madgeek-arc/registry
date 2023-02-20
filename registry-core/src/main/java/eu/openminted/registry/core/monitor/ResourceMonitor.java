package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ServiceException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by antleb on 5/26/16.
 */
@Aspect
@Component
public class ResourceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);

    private final List<ResourceListener> resourceListeners;
    private final List<ResourceTypeListener> resourceTypeListeners;
    private final ResourceDao resourceDao;

    public ResourceMonitor(List<ResourceListener> resourceListeners, List<ResourceTypeListener> resourceTypeListeners,
                           ResourceDao resourceDao) {
        this.resourceListeners = resourceListeners;
        this.resourceTypeListeners = resourceTypeListeners;
        this.resourceDao = resourceDao;
    }

    @Around("execution (* eu.openminted.registry.core.service.ResourceService.addResource(eu.openminted.registry.core.domain.Resource)) && args(resource)")
    public Resource resourceAdded(ProceedingJoinPoint pjp, Resource resource) throws Throwable {
        try {
            resource = (Resource) pjp.proceed();

            for (ResourceListener listener : resourceListeners) {
                try {
                    listener.resourceAdded(resource);
                    logger.debug("Notified listener : " + listener.getClass().getSimpleName() + " for create");
                } catch (Exception e) {
                    logger.error("Error notifying listener", e);
                }
            }
        } catch (Exception e) {
            logger.error("fatal error in monitor", e);
            throw e;
        }
        return resource;

    }

    @Around("execution (* eu.openminted.registry.core.service.ResourceService.updateResource(eu.openminted.registry.core.domain.Resource)) && args(resource)")
    public Resource resourceUpdated(ProceedingJoinPoint pjp, Resource resource) throws Throwable {

        try {
            if (resource.getId() == null || resource.getId().isEmpty()) {
                throw new ServiceException("Empty resource ID");
            }

            Resource previous = resourceDao.getResource(resource.getId());

            Resource temp = new Resource(previous.getId(), previous.getResourceType(), previous.getVersion(), previous.getPayload(), previous.getPayloadFormat());

            pjp.proceed();

            if (resourceListeners != null)
                for (ResourceListener listener : resourceListeners) {
                    try {
                        listener.resourceUpdated(temp, resource);
                        logger.debug("Notified listener : " + listener.getClass().getSimpleName() + " for update");
                    } catch (Exception e) {
                        logger.error("Error notifying listener", e);
                    }
                }
        } catch (Exception e) {
            logger.error("fatal error in monitor", e);
            throw e;
        }
        return resource;
    }

    @Around(("execution (* eu.openminted.registry.core.service.ResourceService.deleteResource(..)) && args(resourceId)"))
    public void resourceDeleted(ProceedingJoinPoint pjp, String resourceId) throws Throwable {

        Resource previous = resourceDao.getResource(resourceId);

        pjp.proceed();

        for (ResourceListener listener : resourceListeners) {
            try {
                listener.resourceDeleted(previous);
            } catch (Exception e) {
                logger.error("Error notifying listener", e);
            }
        }

    }

    @Around("execution (* eu.openminted.registry.core.service.ResourceTypeService.addResourceType(eu.openminted.registry.core.domain.ResourceType)) && args(resourceType)")
    public ResourceType resourceTypeAdded(ProceedingJoinPoint pjp, ResourceType resourceType) throws Throwable {

        pjp.proceed();


        if (resourceTypeListeners != null)
            for (ResourceTypeListener listener : resourceTypeListeners) {
                try {
                    listener.resourceTypeAdded(resourceType);
                } catch (Exception e) {
                    logger.error("Error notifying listener", e);
                }
            }
        return resourceType;
    }

    @Around("execution (* eu.openminted.registry.core.service.ResourceTypeService.deleteResourceType(String)) && args(name)")
    public void resourceTypeDeleted(ProceedingJoinPoint pjp, String name) throws Throwable {

        pjp.proceed();

        if (resourceTypeListeners != null)
            for (ResourceTypeListener listener : resourceTypeListeners) {
                try {
                    listener.resourceTypeDelete(name);
                } catch (Exception e) {
                    logger.error("Error notifying listener", e);
                }
            }
    }
}
