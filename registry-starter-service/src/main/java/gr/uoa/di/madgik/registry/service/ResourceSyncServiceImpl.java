/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.service.ResourceSyncService;
import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.dao.VersionDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.resourcesync.domain.CapabilityList;
import gr.uoa.di.madgik.registry.resourcesync.domain.ChangeList;
import gr.uoa.di.madgik.registry.resourcesync.domain.ResourceList;
import gr.uoa.di.madgik.registry.resourcesync.domain.URL;
import gr.uoa.di.madgik.registry.resourcesync.domain.ResourceSync;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service("resourceSyncService")
@Transactional(readOnly = true)
public class ResourceSyncServiceImpl implements ResourceSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceSyncServiceImpl.class);

    private final ResourceTypeDao resourceTypeDao;
    private final ResourceDao resourceDao;
    private final VersionDao versionDao;
    private String host;

    public ResourceSyncServiceImpl(Environment environment, ResourceTypeDao resourceTypeDao,
                                   ResourceDao resourceDao, VersionDao versionDao) {
        this.host = environment.getRequiredProperty("registry.host");
        if (this.host == null) {
            throw new RuntimeException("Missing property 'registry.host'");
        }
        this.resourceTypeDao = resourceTypeDao;
        this.resourceDao = resourceDao;
        this.versionDao = versionDao;
    }

    @Override
    public ResourceList getResourceList(String resourceType) {

        ResourceList resourceList = new ResourceList();
        resourceTypeDao.getResourceType(resourceType).getResources().stream().forEach(resource -> {
            URL entry = new URL();
            entry.setLoc(host + "/resources/" + resourceType + "/" + resource.getId(), resource.getModificationDate());
            resourceList.addUrl(entry);
        });

        return resourceList;
    }

    @Override
    public CapabilityList getCapabilityList() {

        CapabilityList capabilityList = new CapabilityList();

        List<ResourceType> rtList = resourceTypeDao.getAllResourceType();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date oneMonth = calendar.getTime();

        rtList.forEach(rT -> {
            capabilityList.addCapableUrl(host + "/resourcesync/" + rT.getName() + "/resourcelist.xml", ResourceSync.CAPABILITY_RESOURCELIST);
            capabilityList.addCapableUrl(host + "/resourcesync/" + rT.getName() + "/" + oneMonth.getTime() + "/changelist.xml", ResourceSync.CAPABILITY_CHANGELIST);
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
            resourcesHash.put(resource.getId(), resource);
            changeList.addChange(host + "/resources/" + resource.getResourceTypeName() + "/" + resource.getId(), resource.getModificationDate(), ResourceSync.CHANGE_CREATED);
        });

        resources = resourceDao.getModifiedSince(date, resourceType);

        resources.forEach(resource -> {
            if (!resourcesHash.containsKey(resource.getId())) {
                changeList.addChange(host + "/resources/" + resource.getResourceTypeName() + "/" + resource.getId(), resource.getModificationDate(), ResourceSync.CHANGE_UPDATED);
            }
        });

        versionDao.getOrphans().forEach(version -> {
            changeList.addChange(host + "/resources/" + version.getResourceType().getName() + "/" + version.getParentId(), version.getCreationDate(), ResourceSync.CHANGE_DELETED);
        });

        return changeList;
    }

    @PostConstruct
    public void onConstruct() {
        host = (host.substring(host.length() - 1).equals("/") ? host.substring(0, host.length() - 1) : host);
        System.out.println(host);
    }
}

