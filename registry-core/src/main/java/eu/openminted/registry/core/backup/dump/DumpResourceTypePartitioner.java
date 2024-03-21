package eu.openminted.registry.core.backup.dump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@StepScope
public class DumpResourceTypePartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(DumpResourceTypePartitioner.class);
    @Value("#{jobExecutionContext}")
    Properties jobExecution;
    @Value("#{jobExecutionContext['addedResourceTypes']}")
    private List<String> resourceTypes;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> versionMap = new HashMap<>(gridSize);
        for (String resourceType : resourceTypes) {
            ExecutionContext context = new ExecutionContext();
            context.put("resourceType", resourceType);
            versionMap.put(resourceType, context);
        }
        return versionMap;
    }
}
