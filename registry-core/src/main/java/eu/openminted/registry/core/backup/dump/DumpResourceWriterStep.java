package eu.openminted.registry.core.backup.dump;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static eu.openminted.registry.core.backup.dump.DumpResourceTypeStep.PERMISSIONS;


@Component
@StepScope
public class DumpResourceWriterStep implements ItemWriter<Resource>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(DumpResourceWriterStep.class);

    private boolean raw;

    private String resourceTypeName;

    private Path resourceTypeDirectory;

    private ObjectMapper objectMapper;

    private boolean versions;

    @Override
    public void write(List<? extends Resource> items) throws Exception {
        for (Resource resource : items) {
            storeResource(resource);
            if (versions) {
                storeVersions(resource);
            }
        }
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        resourceTypeName = stepExecution.getExecutionContext().getString("resourceType");
        raw = Boolean.parseBoolean(stepExecution.getJobExecution().getJobParameters().getString("raw"));
        versions = Boolean.parseBoolean(stepExecution.getJobExecution().getJobParameters().getString("versions"));
        String directory = stepExecution.getJobExecution().getExecutionContext().getString("directory");
        resourceTypeDirectory = Paths.get(directory, resourceTypeName);
        objectMapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return ExitStatus.COMPLETED;
    }

    private void storeResource(Resource resource) throws IOException {
        String extension = ".json";
        if (raw)
            extension = "." + resource.getPayloadFormat();
        File openFile = new File(resourceTypeDirectory.toFile(), resource.getId() + extension);
        Path filePath = Files.createFile(openFile.toPath(), PERMISSIONS);
        FileWriter file = new FileWriter(filePath.toFile());
        resource.setIndexedFields(null);
        if (raw) {
            file.write(resource.getPayload());
        } else {
            file.write(objectMapper.writeValueAsString(resource));
        }
        file.flush();
        file.close();
    }

    private void storeVersions(Resource resource) throws IOException {
        if (resource.getVersions() == null || resource.getVersions().isEmpty())
            return;
        File versionDir = new File(resourceTypeDirectory + "/" + resource.getId() + "-version");
        if (!versionDir.exists()) {
            Files.createDirectory(versionDir.toPath(), PERMISSIONS);
        }
        for (Version version : resource.getVersions()) {
            File openFileVersion = new File(versionDir, version.getId() + ".json");
            Path filePathVersion = Files.createFile(openFileVersion.toPath(), PERMISSIONS);
            FileWriter fileVersion = new FileWriter(filePathVersion.toFile());
            fileVersion.write(objectMapper.writeValueAsString(version));
            fileVersion.flush();
            fileVersion.close();
        }
    }

}
