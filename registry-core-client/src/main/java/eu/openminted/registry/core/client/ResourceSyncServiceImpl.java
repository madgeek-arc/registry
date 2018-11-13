package eu.openminted.registry.core.client;

import eu.openminted.registry.core.resourcesync.domain.CapabilityList;
import eu.openminted.registry.core.resourcesync.domain.ChangeList;
import eu.openminted.registry.core.resourcesync.domain.ResourceList;
import eu.openminted.registry.core.service.ResourceSyncService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("resourceSyncService")
public class ResourceSyncServiceImpl implements ResourceSyncService {


    @Override
    public ResourceList getResourceList(String resourceType) {
        return null;
    }

    @Override
    public CapabilityList getCapabilityList() {
        return null;
    }

    @Override
    public ChangeList getChangeList(String resourceType, Date date) {
        return null;
    }
}