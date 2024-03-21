package eu.openminted.registry.core.backup;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.DumpService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import joptsimple.internal.Strings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service("dumpService")
public class DumpServiceImpl implements DumpService {

    private static final Logger logger = LoggerFactory.getLogger(DumpServiceImpl.class);

    @Autowired
    JobLauncher mySyncJobLauncher;

    @Autowired
    Job dumpJob;

    @Autowired
    ResourceTypeService resourceTypeService;

    private static File pack(String sourceDirPath) throws IOException {
        Path p = Files.createTempFile("dump-", "-" + new Date().getTime());
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
                            logger.error(e.getMessage(), e);
                        }
                    });
        }
        return p.toFile();
    }

    @Override
    public File dump(boolean isRaw, boolean wantSchema, String[] resourceTypes, boolean wantVersion) {

        String resourceTypesList;
        JobExecution job;
        if (resourceTypes.length == 0)
            resourceTypesList = resourceTypeService.getAllResourceType().stream().map(ResourceType::getName).collect(Collectors.joining(","));
        else
            resourceTypesList = Strings.join(resourceTypes, ",");

        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addDate("date", new Date());
        builder.addString("resourceTypes", resourceTypesList);
        builder.addString("save", Boolean.toString(wantSchema));
        builder.addString("raw", Boolean.toString(isRaw));
        builder.addString("versions", Boolean.toString(wantVersion));
        try {
            job = mySyncJobLauncher.run(dumpJob, builder.toJobParameters());
        } catch (Exception e) {
            throw new ServiceException(e);
        }

        if (logger.isDebugEnabled()) {
            Throwable ex = job.getAllFailureExceptions().stream().reduce(new Exception(), (e1, e2) -> {
                e1.addSuppressed(e2);
                return e1;
            });
            logger.debug("All Job Failures: ", ex);
        }

        String directory = job.getExecutionContext().getString("directory");

        File zip;
        File contents = new File(directory);
        try {
            zip = pack(directory);
            FileUtils.cleanDirectory(contents);
        } catch (IOException e) {
            throw new ServiceException(e);
        } finally {
            if (!contents.delete()) {
                logger.warn("Could not delete temporary folder '{}'", contents.getAbsolutePath());
            }
        }
        return zip;
    }
}
