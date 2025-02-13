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

import java.io.File;
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
        File openFile = new File(resourceTypeDirectory.toFile(), resource.getId() + extension);
        resource.setIndexedFields(null);
        if (raw) {
            Files.write(openFile.toPath(), objectMapper.writeValueAsBytes(resource.getPayload()),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            Files.write(openFile.toPath(), objectMapper.writeValueAsBytes(resource),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void storeVersions(Resource resource) throws IOException {
        if (resource.getVersions() == null || resource.getVersions().isEmpty())
            return;
        File versionDir = new File(resourceTypeDirectory + "/" + resource.getId() + "-version");
        if (!versionDir.exists()) {
            Files.createDirectory(versionDir.toPath(), DumpResourceTypeStep.PERMISSIONS);
        }
        for (Version version : resource.getVersions()) {
            File openFileVersion = new File(versionDir, version.getId() + ".json");
            Files.write(openFileVersion.toPath(), objectMapper.writeValueAsBytes(version),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

}
