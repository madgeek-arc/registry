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

import gr.uoa.di.madgik.registry.dao.VersionDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("versionService")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional(readOnly = true)
public class VersionServiceImpl implements VersionService {

    private static Logger logger = LoggerFactory.getLogger(VersionServiceImpl.class);

    private final VersionDao versionDao;
    private final ResourceService resourceService;
    private final ResourceTypeService resourceTypeService;

    public VersionServiceImpl(VersionDao versionDao, ResourceService resourceService,
                              ResourceTypeService resourceTypeService) {
        this.versionDao = versionDao;
        this.resourceService = resourceService;
        this.resourceTypeService = resourceTypeService;
    }

    @Override
    public Version getVersion(String resource_id, String version) {
        Resource resource = resourceService.getResource(resource_id);
        if (resource == null)
            return null;
        else
            return versionDao.getVersion(resource, version);
    }

    @Override
    public List<Version> getVersionsByResource(String resource_id) {
        Resource resource = resourceService.getResource(resource_id);
        if (resource == null)
            return null;
        else
            return versionDao.getVersionsByResource(resource);
    }

    @Override
    public List<Version> getVersionsByResourceType(String resourceType_name) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceType_name);
        if (resourceType == null)
            return null;
        else
            return versionDao.getVersionsByResourceType(resourceType);
    }

    @Override
    public List<Version> getAllVersions() {
        return versionDao.getAllVersions();
    }

}

