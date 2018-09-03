package eu.openminted.registry.core.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.VersionDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.elasticsearch.service.ElasticOperationsService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.RestoreService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static javax.xml.bind.JAXBContext.newInstance;


@Service("restoreService")
@Transactional
public class RestoreServiceImpl implements RestoreService {

    private static final Logger logger = LogManager.getLogger(RestoreServiceImpl.class);

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    ResourceDao resourceDao;

    @Autowired
    ResourceService resourceService;

    @Autowired
    VersionDao versionDao;

    @Autowired
    ElasticOperationsService elasticOperationsService;

    private JAXBContext jaxbContext = null;

    @Override
    public void restoreDataFromZip(MultipartFile file) {
        /**
         * save file to temp
         */
        try {
            File zip = File.createTempFile(UUID.randomUUID().toString(), "temp");
            FileOutputStream o = new FileOutputStream(zip);
            IOUtils.copy(file.getInputStream(), o);
            o.close();

            UnzipUtility unzipUtility = new UnzipUtility();
            Path tempDirPath = Files.createTempDirectory("decompress");
            File tempDirFile = tempDirPath.toFile();

            unzipUtility.unzip(zip.getAbsolutePath(),tempDirPath.toString());

            HashMap<String, List<Version>> versions = new HashMap<>();
            HashMap<String, Resource> oldResourcesIds = new HashMap<>();

            storeResources(tempDirFile, versions, oldResourcesIds);
            List<Resource> bulkResources = new ArrayList<>();
            for(Map.Entry<String, List<Version>> entry : versions.entrySet()){

                for(Version version : entry.getValue()) {
                    Resource resource = oldResourcesIds.get(entry.getKey());

                    version.setResource(resource);
                    if(entry.getValue().equals(resource.getId()))
                        versionDao.addVersion(version);
                }

            }
            for(Map.Entry<String, Resource> resourceEntry : oldResourcesIds.entrySet())
                bulkResources.add(resourceEntry.getValue());

            elasticOperationsService.addBulk(bulkResources);

            zip.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void storeResources(File dir, HashMap<String, List<Version>> versions, HashMap<String, Resource> oldResourcesIds) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if(file.isDirectory()) {
                logger.debug("Extracting files from " + file.getName());
                storeResources(file, versions, oldResourcesIds);
            }else {
                if (FilenameUtils.removeExtension(file.getName()).equals("schema")) {
                    //if there is a file with the same name as the directory then it's the schema of the resource type. Drop resource type and reimport
                    String resourceTypeName = file.getParentFile().getName();
                    logger.debug("Resource type:"+resourceTypeName + ", starting procedure");
                    if(resourceTypeService.getResourceType(resourceTypeName)!=null) {
                        logger.debug("Resource type is present, deleting it..");
                        resourceTypeService.deleteResourceType(resourceTypeName);
                    }

                    ResourceType resourceType = new ResourceType();
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        logger.debug("Reading resource type from file..");
                        resourceType = mapper.readValue(FileUtils.readFileToString(file).replaceAll("^\t$", "").replaceAll("^\n$",""), ResourceType.class);
                        logger.debug("Adding resource type");
                        if(resourceType.getSchemaUrl()!=null || !resourceType.getSchemaUrl().isEmpty())
                            resourceType.setSchema(null);

                        resourceTypeService.addResourceType(resourceType);
                    } catch (IOException e) {
                        throw new ServiceException("Failed to read schema file");
                    }
                }
            }
        }
        try {
            jaxbContext =  newInstance(Resource.class);
        } catch (JAXBException e) {
            throw new ServiceException("Could not initiate jaxbContext for RESOURCE class",e);
        }
        for(File file : files){
            try {
                if(!file.isDirectory() && !file.getAbsolutePath().contains("version")) {
                    logger.debug("Scanning resource file with name "+ file.getName());

                    if(!FilenameUtils.removeExtension(file.getName()).equals("schema")){
                        //if it's not the schema file then add it as a resource
                        logger.debug("Adding resource:"+file.getName());
                        String extension = FilenameUtils.getExtension(file.getName());
                        Resource resource = new Resource();
                        if(extension.equals("json")) {
                            resource = deserializeResource(file, extension);
                            if(resource==null)
                                resource = deserializeResource(file, "xml");
                        }else if(extension.equals("xml")){
                            resource = deserializeResource(file, extension);
                            if(resource==null)
                                resource = deserializeResource(file, "json");
                        }else{
                            throw new ServiceException("Unsupported file format");
                        }


                        ResourceType resourceType = resourceTypeService.getResourceType(file.getParentFile().getName());
                        if(resource==null) {//if it's still null that means that the file contains just the payload
                            resource = new Resource();
                            resource.setPayload(FileUtils.readFileToString(file));
                            resource.setPayloadFormat(extension);
                            resource.setResourceType(resourceType);
                            oldResourcesIds.put(FilenameUtils.removeExtension(file.getName()), resourceService.addResource(resource));
                        }else{
                            resource.setResourceType(resourceType);
                            resourceDao.addResource(resource);// we are using the DAO service in order to keep the previous ID of the resource
                            oldResourcesIds.put(FilenameUtils.removeExtension(file.getName()), resource);
                        }

                    }else{
                        logger.debug("Resource "+file.getName()+" insertion postponed");
                    }
                }else if(!file.isDirectory() && file.getAbsolutePath().contains("version")) {
                    Version version = deserializeVersion(file);

                    if(version==null)
                        throw new ServiceException("Could not deserialize version");

                    String resourceId = file.getParentFile().getName().replace("-version","");
                    if(versions.containsKey(resourceId)) {
                        versions.get(resourceId).add(version);
                    }else{
                        versions.put(resourceId, new ArrayList<>());
                        versions.get(resourceId).add(version);
                    }
                }
                logger.debug(file.getName() + " ---- Just gone through parsing");
            } catch (IOException e) {
               throw new ServiceException("Error parsing resource file",e);
            }

        }
    }

    private Resource deserializeResource(File file, String mediaType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (mediaType.equals("json"))
                return mapper.readValue(file, Resource.class);
            else if (mediaType.equals("xml")) {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                return (Resource) unmarshaller.unmarshal(file);
            }else
                return null;
        } catch (IOException | ClassCastException | JAXBException e) {
            logger.error("Error deserializing object",e);
            return null;
        }
    }

    private Version deserializeVersion(File file) {
        ObjectMapper mapper = new ObjectMapper();
        try {
                return mapper.readValue(file, Version.class);
        } catch (IOException | ClassCastException e) {
            logger.error("Error deserializing object",e);
            return null;
        }
    }

    public class UnzipUtility {
        /**
         * Size of the buffer to read/write data
         */
        private static final int BUFFER_SIZE = 4096;
        /**
         * Extracts a zip file specified by the zipFilePath to a directory specified by
         * destDirectory (will be created if does not exists)
         * @param zipFilePath
         * @param destDirectory
         * @throws IOException
         */
        public void unzip(String zipFilePath, String destDirectory) throws IOException {
            File destDir = new File(destDirectory);
            if (!destDir.exists()) {
                destDir.mkdir();
            }
            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                boolean isDir = false;

                String[] splitInto = entry.getName().split("/");

                File tmpFile = null;
                if(splitInto.length<2) {//it's a dir
                    tmpFile = new File(destDirectory + File.separator + splitInto[0]);
                    isDir = true;
                }else if(splitInto.length==2){//no versions included
                    tmpFile = new File(destDirectory + File.separator + splitInto[splitInto.length - 2]);
                }else{//versions included
                    tmpFile = new File(destDirectory + File.separator + splitInto[splitInto.length - 3] + File.separator + splitInto[splitInto.length - 2]);
                }

                if(!tmpFile.exists())
                    tmpFile.mkdirs();

                if(!isDir)
                    extractFile(zipIn, filePath);

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        }
        /**
         * Extracts a zip entry (file entry)
         * @param zipIn
         * @param filePath
         * @throws IOException
         */
        private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
            bos.close();
        }


    }


}
