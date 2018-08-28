package eu.openminted.registry.core.backup.dump;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@StepScope
public class DumpResourceTypeStep implements Tasklet, StepExecutionListener {

    public static final FileAttribute PERMISSIONS = PosixFilePermissions.asFileAttribute(EnumSet.of
            (PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission
                            .OWNER_EXECUTE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission
                            .OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));

    private static final Logger logger = LogManager.getLogger(DumpResourceTypeStep.class);

    private ResourceTypeService resourceTypeService;

    private boolean saveSchema;

    private List<String> resourceTypeNames;

    private Path masterDirectory;

    private static final String FILENAME_FOR_SCHEMA = "schema.json";

    private ObjectMapper mapper;

    private List<String> stepResourceTypes = new ArrayList<>();

    @Autowired
    DumpResourceTypeStep(ResourceTypeService resourceTypeService) {
        this.resourceTypeService = resourceTypeService;
        this.saveSchema = true;
        mapper = new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String resourceTypes = stepExecution.getJobExecution().getJobParameters().getString("resourceTypes");
        saveSchema = Boolean.parseBoolean(stepExecution.getJobExecution().getJobParameters().getString("save"));
        resourceTypeNames = new ArrayList<>(Arrays.asList(resourceTypes.split(",")));
        masterDirectory = createBasicPath();
        stepExecution.getJobExecution().getExecutionContext().putString("directory",masterDirectory.toString());
        logger.info(masterDirectory.toString());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        stepExecution.getJobExecution().getExecutionContext().put("addedResourceTypes",stepResourceTypes);
        if(saveSchema)
            return ExitStatus.COMPLETED;
        else
            return ExitStatus.NOOP;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if(resourceTypeNames.isEmpty())
            return RepeatStatus.FINISHED;
        String resourceTypeName = resourceTypeNames.remove(0);
        Path resourceTypePath = Files.createDirectory(Paths.get(masterDirectory.toString(),resourceTypeName),PERMISSIONS);
        logger.info("Saving " + resourceTypeName);
        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        stepResourceTypes.add(resourceTypeName);
        if(!saveSchema)
            return RepeatStatus.FINISHED;
        resourceType.setSchema(resourceType.getSchema());
        Path tempFile = Paths.get(resourceTypePath.toString(),FILENAME_FOR_SCHEMA);
        Files.createFile(tempFile,PERMISSIONS);
        FileWriter file = new FileWriter(tempFile.toFile());
        file.write(mapper.writeValueAsString(resourceType));
        file.flush();
        file.close();
        return RepeatStatus.CONTINUABLE;
    }

    private static Path createBasicPath(){
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

}
