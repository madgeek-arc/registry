package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by antleb on 5/26/16.
 */
@Aspect
@Component
public class ResourceMonitor {

	private static Logger logger = Logger.getLogger(ResourceMonitor.class);

	@Autowired(required = false)
	private List<ResourceListener> resourceListeners;

	@Autowired(required = false)
	private List<ResourceTypeListener> resourceTypeListeners;

	@Autowired
	private ResourceDao resourceDao;

	@Around("execution (* eu.openminted.registry.core.service.ResourceService.addResource(eu.openminted.registry.core.domain.Resource)) && args(resource)")
	public void resourceAdded(ProceedingJoinPoint pjp, Resource resource) throws Throwable {

		resource = (Resource) pjp.proceed();

		if (resourceListeners != null)
			for (ResourceListener listener : resourceListeners) {
				try {
					listener.resourceAdded(resource);
				} catch (Exception e) {
					logger.error("Error notifying listener", e);
				}
			}
	}

	@Around("execution (* eu.openminted.registry.core.service.ResourceService.updateResource(eu.openminted.registry.core.domain.Resource)) && args(resource)")
	public void resourceUpdated(ProceedingJoinPoint pjp, Resource resource) throws Throwable {

		Resource previous = resourceDao.getResource(resource.getResourceType(), resource.getId());

		pjp.proceed();

		if (resourceListeners != null)
			for (ResourceListener listener : resourceListeners) {
				try {
					listener.resourceUpdated(previous, resource);
				} catch (Exception e) {
					logger.error("Error notifying listener", e);
				}
			}
	}

	@Around(("execution (* eu.openminted.registry.core.service.ResourceService.deleteResource(java.lang.String)) && args(resourceId)"))
	public void resourceDeleted(ProceedingJoinPoint pjp, String resourceId) throws Throwable {

		Resource previous = resourceDao.getResource(null, resourceId);

		pjp.proceed();

		if (resourceListeners != null)
			for (ResourceListener listener : resourceListeners) {
				try {
					listener.resourceDeleted(previous);
				} catch (Exception e) {
					logger.error("Error notifying listener", e);
				}
			}

	}

	@Around("execution (* eu.openminted.registry.core.service.ResourceTypeService.addResourceType(eu.openminted.registry.core.domain.ResourceType)) && args(resourceType)")
	public void resourceTypeAdded(ProceedingJoinPoint pjp, ResourceType resourceType) throws Throwable {

		pjp.proceed();

		if (resourceTypeListeners != null)
			for (ResourceTypeListener listener : resourceTypeListeners) {
				try {
					listener.resourceTypeAdded(resourceType);
				} catch (Exception e) {
					logger.error("Error notifying listener", e);
				}
			}
	}
}