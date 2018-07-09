package eu.openminted.registry.core.backup;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.service.ServiceException;

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

public class RegistryFileUtils {

    public static final FileAttribute PERMISSIONS = PosixFilePermissions.asFileAttribute(EnumSet.of
            (PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission
                            .OWNER_EXECUTE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission
                            .OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));


    public static void createDirectory(String name, List<Resource> resources, boolean isRaw, boolean wantVersion) {
        File parentDirectory = new File(name);

        if (!parentDirectory.exists()) {
            try {
                Files.createDirectory(parentDirectory.toPath(), PERMISSIONS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Resource resource : resources) {
            try {
                String extension = ".json";

                if (isRaw)
                    extension = "." + resource.getPayloadFormat();

                File openFile = new File(name + "/" + resource.getId() + extension);

                Path filePath = Files.createFile(openFile.toPath(), PERMISSIONS);
                FileWriter file = new FileWriter(filePath.toFile());
                resource.setIndexedFields(new ArrayList<>());
                resource.setResourceTypeName(resource.getResourceType().getName());
                if (isRaw) {
                    file.write(resource.getPayload());
                } else {
                    ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
                    file.write(mapper.writeValueAsString(resource));
                }
                file.flush();
                file.close();

                if(wantVersion) {
                    for (Version version : resource.getVersions()) {
                        File versionDir = new File(name + "/" + resource.getId() + "-version");
                        if (!versionDir.exists()) {
                            Files.createDirectory(versionDir.toPath(), PERMISSIONS);
                        }
                        File openFileVersion = new File(name + "/" + resource.getId() + "-version/" + version.getId() + ".json");
                        Path filePathVersion = Files.createFile(openFileVersion.toPath(), PERMISSIONS);
                        FileWriter fileVersion = new FileWriter(filePathVersion.toFile());
                        ObjectMapper mapperVersion = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
                        fileVersion.write(mapperVersion.writeValueAsString(version));
                        fileVersion.flush();
                        fileVersion.close();

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceException("Failed to create file(s) for " + name);

            }
        }
    }

    static File finalizeFile(Path masterDirectory, List<File> fileList){
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



    public static void writeZipFile(File directoryToZip, List<File> fileList) {

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

    public static Path createBasicPath(){
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

    public static String getCurrentDate() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("ddMMyyyy");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    public static void getAllFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles();
        for (File file : files) {
            fileList.add(file);
            if (file.isDirectory()) {
                getAllFiles(file, fileList);
            }
        }
    }

    public static void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
            IOException {


        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String[] splitInto = file.getCanonicalPath().substring(System.getProperty("java.io.tmpdir").length()).split("/");
        String zipFilePath = "";

        if(splitInto.length<5)
            zipFilePath = splitInto[splitInto.length-2]+"/"+ splitInto[splitInto.length-1];
        else
            zipFilePath = splitInto[splitInto.length-3]+"/" +splitInto[splitInto.length-2]+"/"+ splitInto[splitInto.length-1];

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

}
