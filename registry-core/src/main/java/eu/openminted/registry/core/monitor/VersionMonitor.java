package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.VersionDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class VersionMonitor implements ResourceListener {

	private static Logger logger = LoggerFactory.getLogger(VersionMonitor.class);

	private final VersionDao versionDao;

	public VersionMonitor(VersionDao versionDao) {
		this.versionDao = versionDao;
	}

	@Override
	public void resourceAdded(Resource resource) {
		createVersion(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		//previousResource.id==newResource.id so we can send either one
		createVersion(newResource);
	}

	@Override
	public void resourceChangedType(Resource resource, ResourceType previousResourceType, ResourceType resourceType) {
		versionDao.updateParent(resource, previousResourceType, resource.getResourceType());
	}

	@Override
	public void resourceDeleted(Resource resource) {
		logger.info("Deleting resource with id: {}", resource.getId());
	}

	private void createVersion(Resource newResource) {

		Version version = new Version();

		version.setCreationDate(new Date());
		version.setId(UUID.randomUUID().toString());
		version.setPayload(newResource.getPayload());
		version.setResource(newResource);
		version.setResourceType(newResource.getResourceType());
		version.setVersion(newResource.getVersion());
		version.setParentId(newResource.getId());
		version.setResourceTypeName(newResource.getResourceTypeName());

		versionDao.addVersion(version);
	}
}
