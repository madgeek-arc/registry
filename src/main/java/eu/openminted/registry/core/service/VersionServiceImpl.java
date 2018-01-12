package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.VersionDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("versionService")
@Transactional
public class VersionServiceImpl implements VersionService {

    private static Logger logger = Logger.getLogger(VersionServiceImpl.class);

    @Autowired
    VersionDao versionDao;

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;


    @Override
    public Version getVersion(String resource_id, String version) {
        Resource resource = resourceService.getResource(null, resource_id);
        if(resource == null)
            return null;
        else
            return versionDao.getVersion(resource,version);
    }

    @Override
    public List<Version> getVersionsByResource(String resource_id) {
        Resource resource = resourceService.getResource(null,resource_id);
        if(resource==null)
            return null;
        else
            return versionDao.getVersionsByResource(resource);
    }

    @Override
    public List<Version> getVersionsByResourceType(String resourceType_name) {
        ResourceType resourceType = resourceTypeService.getResourceType(resourceType_name);
        if(resourceType==null)
            return null;
        else
            return versionDao.getVersionsByResourceType(resourceType);
    }

    @Override
    public List<Version> getAllVersions() {
        return versionDao.getAllVersions();
    }

    @Override
    public void addVersion(Version version) {
        versionDao.addVersion(version);
    }

}

