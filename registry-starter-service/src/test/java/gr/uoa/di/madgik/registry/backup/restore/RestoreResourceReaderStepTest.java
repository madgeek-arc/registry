/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.backup.restore;

import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.index.IndexMapper;
import gr.uoa.di.madgik.registry.index.IndexMapperFactory;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestoreResourceReaderStepTest {

    @TempDir
    private Path tempDir;

    @Test
    void read_converts_legacy_resource_date_timestamps_from_epoch_millis() throws Exception {
        ResourceDao resourceDao = mock(ResourceDao.class);
        IndexMapperFactory indexMapperFactory = mock(IndexMapperFactory.class);
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        IndexMapper indexMapper = mock(IndexMapper.class);

        ResourceType resourceType = new ResourceType();
        resourceType.setName("employee");
        resourceType.setPayloadType("xml");

        Path resourceFile = tempDir.resolve("legacy-date-resource.json");
        Files.writeString(resourceFile, legacyDateResourceJson());

        when(resourceTypeService.getResourceType("employee")).thenReturn(resourceType);
        when(indexMapperFactory.createIndexMapper(resourceType)).thenReturn(indexMapper);
        when(indexMapper.getValues("<employee />", resourceType)).thenReturn(List.of());

        RestoreResourceReaderStep reader = new RestoreResourceReaderStep(resourceDao, indexMapperFactory, resourceTypeService);
        reader.beforeStep(stepExecution(resourceFile));

        Resource resource = reader.read();

        Assertions.assertNotNull(resource);
        Assertions.assertEquals(Instant.parse("2026-04-02T10:15:30Z"), resource.getCreationDate());
        Assertions.assertEquals(Instant.parse("2026-04-03T10:15:30Z"), resource.getModificationDate());
    }

    private StepExecution stepExecution(Path resourceFile) {
        JobExecution jobExecution = new JobExecution(1L, new JobInstance(1L, "restore"), new JobParameters());
        jobExecution.getExecutionContext().putString("resourceTypeName", "employee");
        jobExecution.getExecutionContext().put("resources", new java.io.File[]{resourceFile.toFile()});
        return new StepExecution("restoreResourceStep", jobExecution);
    }

    private String legacyDateResourceJson() {
        return """
                {
                  "id": "legacy-date-resource",
                  "version": "restore-version",
                  "payload": "<employee />",
                  "payloadFormat": "xml",
                  "creationDate": 1775124930000,
                  "modificationDate": 1775211330000
                }
                """;
    }
}
