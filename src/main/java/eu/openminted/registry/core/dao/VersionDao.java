package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;

import java.util.List;

public interface VersionDao {

    Version getVersion(Resource resource, String version);

    List<Version> getVersionsByResource(Resource resource);

    List<Version> getVersionsByResourceType(ResourceType resourceType);

    List<Version> getAllVersions();

    void addVersion(Version version);

    void deleteVersion(Version version);

}
