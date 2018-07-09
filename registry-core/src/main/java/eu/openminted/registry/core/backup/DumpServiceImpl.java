package eu.openminted.registry.core.backup;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.DumpService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Service("dumpService")
@Transactional
public class DumpServiceImpl implements DumpService {

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    private static String FILENAME_FOR_SCHEMA = "schema.json";

    @Override
    public File bringAll(boolean isRaw, boolean wantSchema, String[] resourceTypes, boolean wantVersion) {

        Path masterDirectory = RegistryFileUtils.createBasicPath();

        List<ResourceType> resourceTypesList = new ArrayList<>();
        List<Resource> resources;

        if(resourceTypes.length==0)
            resourceTypesList = resourceTypeService.getAllResourceType();

        for(String resourceType: resourceTypes) {
            ResourceType tmp = resourceTypeService.getResourceType(resourceType);
            if(tmp!=null)
                resourceTypesList.add(tmp);
        }

        List<File> fileList = new ArrayList<>();
        for(ResourceType resourceType: resourceTypesList){
            if (!resourceType.getName().equals("user")) {
                resources = resourceType.getResources();
                RegistryFileUtils.createDirectory(masterDirectory.toAbsolutePath().toString() + "/" + resourceType.getName(), resources, isRaw, wantVersion);
                try {
                    if(wantSchema) { //skip schema creation
                        resourceType.setSchema(resourceType.getOriginalSchema());
                        File tempFile = new File(masterDirectory + "/"+resourceType.getName()+"/" + FILENAME_FOR_SCHEMA);
                        Path filePath = Files.createFile(tempFile.toPath(), RegistryFileUtils.PERMISSIONS);
                        FileWriter file = new FileWriter(filePath.toFile());
                        ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
                        file.write(mapper.writeValueAsString(resourceType));
                        file.flush();
                        file.close();
                    }
                } catch (Exception e) {
                    throw new ServiceException("Failed to create schema-file for " + resourceType.getName());
                }
            }
        }

        return RegistryFileUtils.finalizeFile(masterDirectory,fileList);
    }


}
