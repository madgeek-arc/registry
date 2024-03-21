package gr.uoa.di.madgik.registry.service;

import java.io.File;

public interface DumpService {

    File dump(boolean isRaw, boolean schemaless, String[] resourceTypes, boolean wantVersion);

}
