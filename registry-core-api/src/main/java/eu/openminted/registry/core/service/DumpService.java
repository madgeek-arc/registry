package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.Resource;

import java.io.File;
import java.util.List;

public interface DumpService {

	File bringAll(boolean isRaw, boolean schemaless, String[] resourceTypes, boolean wantVersion);

	void createDirectory(String name, List<Resource> resources, boolean isRaw, boolean wantVersion);
}
