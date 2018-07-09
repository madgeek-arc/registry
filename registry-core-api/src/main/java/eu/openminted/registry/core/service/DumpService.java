package eu.openminted.registry.core.service;

import java.io.File;

public interface DumpService {

	File bringAll(boolean isRaw, boolean schemaless, String[] resourceTypes, boolean wantVersion);


}
