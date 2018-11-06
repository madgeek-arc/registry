package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Version;

import java.util.List;

public interface VersionService {

	Version getVersion(String resource_id, String version);

	List<Version> getVersionsByResource(String resource_id);

	List<Version> getVersionsByResourceType(String resourceType_name);

	List<Version> getAllVersions();

}

