package gr.uoa.di.madgik.registry.service;


import gr.uoa.di.madgik.registry.resourcesync.domain.CapabilityList;
import gr.uoa.di.madgik.registry.resourcesync.domain.ChangeList;
import gr.uoa.di.madgik.registry.resourcesync.domain.ResourceList;

import java.util.Date;

public interface ResourceSyncService {

    ResourceList getResourceList(String resourceType);

    CapabilityList getCapabilityList();

    ChangeList getChangeList(String resourceType, Date date);

}