package eu.openminted.registry.core.backup.dump;

import eu.openminted.registry.core.domain.ResourceType;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Component
@StepScope
public class DumpResourceTypePartitioner implements Partitioner {

    private static final Logger logger = LogManager.getLogger(DumpResourceTypePartitioner.class);

    @Value("#{jobExecutionContext['addedResourceTypes']}")
    private List<String> resourceTypes;

    @Value("#{jobExecutionContext}")
    Properties jobExecution;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> versionMap = new HashMap<>(gridSize);
        for(String resourceType : resourceTypes) {
            ExecutionContext context = new ExecutionContext();
            context.put("resourceType", resourceType);
            versionMap.put(resourceType,context);
        }
        return versionMap;
    }
}
