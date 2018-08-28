package eu.openminted.registry.core.backup;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.DumpService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import joptsimple.internal.Strings;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service("dumpService")
public class DumpServiceImpl implements DumpService {

    @Autowired
    JobLauncher mySyncJobLauncher;

    @Autowired
    Job dumpJob;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Override
    public File bringAll(boolean isRaw, boolean wantSchema, String[] resourceTypes, boolean wantVersion) {

        String resourceTypesList;
        JobExecution job;
        if(resourceTypes.length==0)
            resourceTypesList = resourceTypeService.getAllResourceType().stream().map(ResourceType::getName).collect(Collectors.joining(","));
        else
            resourceTypesList = Strings.join(resourceTypes,",");

        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addDate("date",new Date());
        builder.addString("resourceTypes",resourceTypesList);
        builder.addString("save",Boolean.toString(wantSchema));
        builder.addString("raw",Boolean.toString(isRaw));
        builder.addString("versions",Boolean.toString(wantVersion));
        try {
            job = mySyncJobLauncher.run(dumpJob,builder.toJobParameters());
        } catch (Exception e) {
           throw new ServiceException(e);
        }
        String directory = job.getExecutionContext().getString("directory");

        try {
            return pack(directory);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    private static File pack(String sourceDirPath) throws IOException {
        Path p = Files.createTempFile("dump", new Date().toString());
//        Path p = Files.createFile(zipFilePath);
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }
        return p.toFile();
    }
}
