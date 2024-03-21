package eu.openminted.registry.core.service;

import java.io.File;

public interface DumpService {

    File dump(boolean isRaw, boolean schemaless, String[] resourceTypes, boolean wantVersion);

}
