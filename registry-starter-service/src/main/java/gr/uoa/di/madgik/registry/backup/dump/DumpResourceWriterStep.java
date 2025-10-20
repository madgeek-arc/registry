/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.backup.dump;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

@Component
@StepScope
public class DumpResourceWriterStep implements ItemWriter<Resource>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(DumpResourceWriterStep.class);

    private boolean raw;

    private Path resourceTypeDirectory;

    private final ObjectMapper objectMapper;

    private boolean versions;

    public DumpResourceWriterStep() {
        this.objectMapper = JsonMapper
                .builder()
                .configure(MapperFeature.USE_ANNOTATIONS, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .build();
    }

    @Override
    public void write(Chunk<? extends Resource> chunk) throws Exception {
        if (!chunk.isEmpty()) {
            for (Resource resource : chunk) {
                storeResource(resource);
                if (versions) {
                    storeVersions(resource);
                }
            }
        }
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String resourceTypeName = stepExecution.getExecutionContext().getString("resourceType");
        raw = Boolean.parseBoolean(stepExecution.getJobExecution().getJobParameters().getString("raw"));
        versions = Boolean.parseBoolean(stepExecution.getJobExecution().getJobParameters().getString("versions"));
        String directory = stepExecution.getJobExecution().getExecutionContext().getString("directory");
        resourceTypeDirectory = Paths.get(directory, resourceTypeName);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        for (StepExecution execution : stepExecution.getJobExecution().getStepExecutions()) {
            logger.debug("Step Name: " + execution.getStepName() + ", Status: " + execution.getStatus());
            if (execution.getExitStatus().equals(ExitStatus.FAILED)) {
                logger.error("Partition failed: {}\n{}",
                        execution.getExitStatus().getExitDescription(),
                        execution.getFailureExceptions()
                                .stream()
                                .map(Throwable::getMessage)
                                .collect(Collectors.joining("\n")));
            }
        }
        return ExitStatus.COMPLETED;
    }

    private void storeResource(Resource resource) throws IOException {
        String extension = ".json";
        if (raw)
            extension = "." + resource.getPayloadFormat();
        Path path = resourceTypeDirectory.resolve(resource.getId() + extension);
        Files.createDirectories(path.getParent());
        resource.setIndexedFields(null);
        byte[] data;
        if (raw) {
            data = objectMapper.writeValueAsBytes(resource.getPayload());
        } else {
            data = objectMapper.writeValueAsBytes(resource);
        }
        Files.write(path, data,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private void storeVersions(Resource resource) throws IOException {
        if (resource.getVersions() == null || resource.getVersions().isEmpty())
            return;
        Path versionPath = resourceTypeDirectory.resolve(resource.getId() + "-version");
        Files.createDirectories(versionPath);

        for (Version version : resource.getVersions()) {
            Path openFileVersion = versionPath.resolve(version.getId() + ".json");
            Files.write(openFileVersion, objectMapper.writeValueAsBytes(version),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        }
    }

}
