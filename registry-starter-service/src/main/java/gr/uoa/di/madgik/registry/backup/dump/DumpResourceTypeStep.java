package gr.uoa.di.madgik.registry.backup.dump;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@StepScope
public class DumpResourceTypeStep implements Tasklet, StepExecutionListener {

    public static final FileAttribute<Set<PosixFilePermission>> PERMISSIONS = PosixFilePermissions
            .asFileAttribute(Set.of(PosixFilePermission.values()));

    private static final Logger logger = LoggerFactory.getLogger(DumpResourceTypeStep.class);
    private static final String FILENAME_FOR_SCHEMA = "schema.json";
    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper;
    private final List<String> stepResourceTypes = new ArrayList<>();

    private boolean saveSchema;
    private List<String> resourceTypeNames;
    private Path masterDirectory;

    DumpResourceTypeStep(ResourceTypeService resourceTypeService) {
        this.resourceTypeService = resourceTypeService;
        this.saveSchema = true;
        this.mapper = JsonMapper
                .builder()
                .configure(MapperFeature.USE_ANNOTATIONS, true)
                .build();
    }

    private static Path createBasicPath() {
        Path masterDirectory = null;
        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
            masterDirectory = Files.createTempDirectory(dateFormat.format(today.getTime()), PERMISSIONS);
        } catch (IOException e1) {
            throw new ServiceException(e1.getMessage());
        }

        return masterDirectory;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String resourceTypes = stepExecution.getJobExecution().getJobParameters().getString("resourceTypes");
        saveSchema = Boolean.parseBoolean(stepExecution.getJobExecution().getJobParameters().getString("save"));
        resourceTypeNames = new ArrayList<>(Arrays.asList(resourceTypes.split(",")));
        masterDirectory = createBasicPath();
        stepExecution.getJobExecution().getExecutionContext().putString("directory", masterDirectory.toString());
        logger.info("Saving temp data to: {}", masterDirectory);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        stepExecution.getJobExecution().getExecutionContext().put("addedResourceTypes", stepResourceTypes);
        if (saveSchema)
            return ExitStatus.COMPLETED;
        else
            return ExitStatus.NOOP;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (resourceTypeNames.isEmpty())
            return RepeatStatus.FINISHED;
        String resourceTypeName = resourceTypeNames.remove(0);
        Path resourceTypePath = Files.createDirectory(Paths.get(masterDirectory.toString(), resourceTypeName), PERMISSIONS);
        logger.info("Saving {}", resourceTypeName);
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        stepResourceTypes.add(resourceTypeName);
        if (!saveSchema)
            return RepeatStatus.CONTINUABLE;
        resourceType.setSchema(resourceType.getSchema());
        Path tempFile = Paths.get(resourceTypePath.toString(), FILENAME_FOR_SCHEMA);
        Files.write(tempFile, mapper.writeValueAsBytes(resourceType),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return RepeatStatus.CONTINUABLE;
    }

}
