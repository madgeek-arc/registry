package eu.openminted.registry.core.version;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.monitor.ResourceListener;
import eu.openminted.registry.core.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;

public class VersionListener implements ResourceListener {

	@Autowired
	VersionService versionService;

	@Override
	public void resourceAdded(Resource resource) {
		createVersion(resource);
	}

	@Override
	public void resourceUpdated(Resource previousResource, Resource newResource) {
		createVersion(newResource);
	}

	@Override
	public void resourceDeleted(Resource resource) {

	}

	private void createVersion(Resource newResource) {
		Version version = new Version();

		version.setCreationDate(new Date());
		version.setId(UUID.randomUUID().toString());
		version.setPayload(newResource.getPayload());
		version.setResource(newResource);
		version.setResourceType(newResource.getResourceType());
		version.setVersion(newResource.getVersion());

		versionService.addVersion(version);
	}
}
