package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.resourcesync.domain.CapabilityList;
import gr.uoa.di.madgik.registry.resourcesync.domain.ChangeList;
import gr.uoa.di.madgik.registry.resourcesync.domain.ResourceList;
import gr.uoa.di.madgik.registry.service.ResourceSyncService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ClientResourceSyncService implements ResourceSyncService {

    @Override
    public ResourceList getResourceList(String resourceType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CapabilityList getCapabilityList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChangeList getChangeList(String resourceType, Date date) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
