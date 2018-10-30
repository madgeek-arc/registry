package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.dao.VersionDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.resourcesync.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service("resourceSyncService")
@Transactional
public class ResourceSyncServiceImpl implements ResourceSyncService {

    private static Logger logger = LogManager.getLogger(ResourceSyncServiceImpl.class);

    @Value("${registry.host}")
    private String host;

    @Autowired
    ResourceTypeDao resourceTypeDao;

    @Autowired
    ResourceDao resourceDao;

    @Autowired
    VersionDao versionDao;

    @Override
    public ResourceList getResourceList(String resourceType) {

        ResourceList resourceList = new ResourceList();
        resourceTypeDao.getResourceType(resourceType).getResources().stream().forEach(resource -> {
            URL entry = new URL();
            entry.setLoc(host + "/resources/"+resourceType + "/" + resource.getId(), resource.getModificationDate());
            resourceList.addUrl(entry);
        });

        return resourceList;
    }

    @Override
    public CapabilityList getCapabilityList() {

        CapabilityList capabilityList = new CapabilityList();

        List<ResourceType> rtList = resourceTypeDao.getAllResourceType();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH,-1);
        Date oneMonth = calendar.getTime();

        rtList.forEach(rT -> {
            capabilityList.addCapableUrl(host+"/resourcesync/"+rT.getName()+"/resourcelist.xml", ResourceSync.CAPABILITY_RESOURCELIST);
            capabilityList.addCapableUrl(host+"/resourcesync/"+ rT.getName()+"/"+oneMonth.getTime()+"/changelist.xml", ResourceSync.CAPABILITY_CHANGELIST);
        });


        return capabilityList;
    }

    @Override
    public ChangeList getChangeList(String resourceType, Date date) {

        ChangeList changeList = new ChangeList();
        changeList.setFrom(date);
        changeList.setUntil(new Date());
        HashMap<String, Resource> resourcesHash = new HashMap<>();
        List<Resource> resources = resourceDao.getCreatedSince(date, resourceType);

        resources.forEach(resource -> {
            resourcesHash.put(resource.getId(),resource);
            changeList.addChange(host+"/resources/"+resource.getResourceTypeName()+"/"+resource.getId(),resource.getModificationDate(),ResourceSync.CHANGE_CREATED);
        });

        resources = resourceDao.getModifiedSince(date,resourceType);

        resources.forEach(resource ->{
            if(!resourcesHash.containsKey(resource.getId())){
                changeList.addChange(host+"/resources/"+resource.getResourceTypeName()+"/"+resource.getId(),resource.getModificationDate(),ResourceSync.CHANGE_UPDATED);
            }
        });

        versionDao.getOrphans().forEach(version -> {
            changeList.addChange(host+"/resources/"+version.getResourceType().getName()+"/"+version.getParentId(),version.getCreationDate(),ResourceSync.CHANGE_DELETED);
        });

        return changeList;
    }
}

