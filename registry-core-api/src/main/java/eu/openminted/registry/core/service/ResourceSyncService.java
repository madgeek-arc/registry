package eu.openminted.registry.core.service;


import eu.openminted.registry.core.resourcesync.domain.CapabilityList;
import eu.openminted.registry.core.resourcesync.domain.ChangeList;
import eu.openminted.registry.core.resourcesync.domain.ResourceList;

import java.util.Date;

public interface ResourceSyncService {

	ResourceList getResourceList(String resourceType);

	CapabilityList getCapabilityList();

	ChangeList getChangeList(String resourceType, Date date);

}