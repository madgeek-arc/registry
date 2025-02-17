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

package gr.uoa.di.madgik.registry.monitor;

import gr.uoa.di.madgik.registry.dao.VersionDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class VersionMonitor implements ResourceListener {

    private static final Logger logger = LoggerFactory.getLogger(VersionMonitor.class);

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
