package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.VersionDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
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

