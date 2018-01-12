package eu.openminted.registry.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service("dumpService")
@Transactional
public class DumpServiceImpl implements DumpService {

    private static final FileAttribute PERMISSIONS = PosixFilePermissions.asFileAttribute(EnumSet.of
            (PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission
                            .OWNER_EXECUTE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission
                            .OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    static void writeZipFile(File directoryToZip, List<File> fileList) {

        try {

            Path filePath = Files.createFile(directoryToZip.toPath(), PERMISSIONS);
            FileOutputStream fos = new FileOutputStream(filePath.toFile());
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }

            zos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void getAllFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles();
        for (File file : files) {
            fileList.add(file);
            if (file.isDirectory()) {
                getAllFiles(file, fileList);
            }
        }
    }

    static void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
            IOException {


        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String[] splitInto = file.getCanonicalPath().split("/");
        String zipFilePath = splitInto[splitInto.length-2]+"/"+ splitInto[splitInto.length-1];

        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }


    public File bringAll(boolean isRaw, boolean wantSchema, String[] resourceTypes) {

        Path masterDirectory = createBasicPath();

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

                // TODO: stream it! Use a custom iterator...
                resources = resourceType.getResources();
                createDirectory(masterDirectory.toAbsolutePath().toString() + "/" + resourceType.getName(), resources, isRaw);
                try {
                    if(wantSchema) { //skip schema creation
                        File tempFile = new File(masterDirectory + "/"+resourceType.getName()+"/" + resourceType.getName() + ".json");
                        Path filePath = Files.createFile(tempFile.toPath(), PERMISSIONS);
                        FileWriter file = new FileWriter(filePath.toFile());
                        ObjectMapper mapper = new ObjectMapper();
                        file.write(mapper.writeValueAsString(resourceType));
                        file.flush();
                        file.close();
                    }
                } catch (Exception e) {
                    throw new ServiceException("Failed to create schema-file for " + resourceType.getName());
                }
            }
        }

        return finalizeFile(masterDirectory,fileList);
    }

    public void createDirectory(String name, List<Resource> resources,boolean isRaw) {
        File parentDirectory = new File(name);

        if (!parentDirectory.exists()) {
            try {
                Files.createDirectory(parentDirectory.toPath(), PERMISSIONS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < resources.size(); i++) {
            try {
                String extension = ".json";
                if(isRaw)
                    extension = "."+resources.get(i).getPayloadFormat();

                File openFile = new File(name + "/" + resources.get(i).getId() + extension);

                Path filePath = Files.createFile(openFile.toPath(), PERMISSIONS);
                FileWriter file = new FileWriter(filePath.toFile());
                resources.get(i).setIndexedFields(new ArrayList<>());
                if(isRaw){
                    file.write(resources.get(i).getPayload());
                }else{
                    ObjectMapper mapper = new ObjectMapper();
                    file.write(mapper.writeValueAsString(resources.get(i)));
                }
                file.flush();
                file.close();


//                for(Version version : resources.get(i).getVersions()){
//                    File versionDir = new File(name+"/versions/");
//                    if(!versionDir.exists()){
//                        System.out.println(versionDir.getAbsolutePath());
//                        Files.createDirectory(versionDir.toPath(), PERMISSIONS);
//                    }
//                    File openFileVersion = new File(name + "/versions/" + version.getId()+ ".json");
//                    System.out.println(openFileVersion.getAbsolutePath());
//                    Path filePathVersion = Files.createFile(openFileVersion.toPath(), PERMISSIONS);
//                    FileWriter fileVersion = new FileWriter(filePathVersion.toFile());
//                    ObjectMapper mapperVersion = new ObjectMapper();
//                    fileVersion.write(mapperVersion.writeValueAsString(version));
//                    fileVersion.flush();
//                    fileVersion.close();
//
//                }

            } catch (Exception e) {
                e.printStackTrace();
				throw new ServiceException("Failed to create file(s) for "+ name);

            }
        }
    }

    private File finalizeFile(Path masterDirectory, List<File> fileList){
        File tempDir = masterDirectory.toFile();
        getAllFiles(tempDir, fileList);
        File masterZip = new File(masterDirectory + "/final.zip");


        writeZipFile(masterZip, fileList);
        try {
            File tempFile = new File(masterDirectory + "/dump-" + getCurrentDate() + ".zip");
            Files.createFile(tempFile.toPath(), PERMISSIONS);
            masterZip.renameTo(tempFile);
            return tempFile;
        } catch (IOException e1) {

        }
        return masterZip;
    }


    private Path createBasicPath(){
        Path masterDirectory = null;
        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd") ;
            masterDirectory = Files.createTempDirectory(dateFormat.format(today.getTime()),PERMISSIONS);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return masterDirectory;
    }

    private String getCurrentDate() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("ddMMyyyy");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

}
