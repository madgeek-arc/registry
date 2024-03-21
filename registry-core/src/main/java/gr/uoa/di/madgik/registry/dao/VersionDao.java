package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;

import java.util.List;

public interface VersionDao {

    Version getVersion(Resource resource, String version);

    List<Version> getVersionsByResource(Resource resource);

    List<Version> getVersionsByResourceType(ResourceType resourceType);

    List<Version> getAllVersions();

    List<Version> getOrphans();

    void addVersion(Version version);

    void updateVersion(Version version);

    void updateParent(Resource resource, ResourceType oldResourceType, ResourceType newResourceType);

    void deleteVersion(Version version);


}
