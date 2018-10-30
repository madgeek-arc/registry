package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class VersionMonitor implements ResourceListener {

	@Autowired
	VersionService versionService;

	@Override
	public void resourceAdded(Resource resource) {
		createVersion(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		//previousResource.id==newResource.id so we can send either one
		createVersion(previousResource);
	}

	@Override
	public void resourceDeleted(Resource resource) {
		Version version = new Version();

		version.setCreationDate(new Date());
		version.setId(UUID.randomUUID().toString());
		version.setPayload(resource.getPayload());
		version.setResource(null);
		version.setResourceType(resource.getResourceType());
		version.setVersion(resource.getVersion());
		version.setParentId(resource.getId());

		versionService.addVersion(version);
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

		versionService.addVersion(version);
	}
}
