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

package gr.uoa.di.madgik.registry.configuration;

import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.backup.dump.DumpResourceReader;
import gr.uoa.di.madgik.registry.backup.dump.DumpResourceWriterStep;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.monitor.ViewResourceTypeListener;
import gr.uoa.di.madgik.registry.service.ResourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.Callable;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = {
        "spring.profiles.active=test"
})
@Sql(scripts = {"/resource_chunk.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class BatchConfigurationTest extends PostgreSqlTestContainerSupport {

    private static final String TEST_RESOURCE_TYPE = "employee_restore";
    private static final String TEST_RESOURCE_ID = "restore-job-resource";
    private static final String LEGACY_DATE_RESOURCE_ID = "legacy-date-resource";

    @MockitoBean
    EmbeddingModel embeddingModel;

    @MockitoBean
    ViewResourceTypeListener viewResourceTypeListener;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    @Qualifier("dumpJob")
    private Job dumpJob;

    @Autowired
    @Qualifier("restoreJob")
    private Job restoreJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ResourceService resourceService;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBatchOverrides {

        @Bean(name = "threadPoolExecutor")
        Callable<TaskExecutor> threadPoolExecutor() {
            return SyncTaskExecutor::new;
        }

        @Bean(name = "resourcesDumpStep")
        Step resourcesDumpStep(JobRepository jobRepository,
                               @Qualifier("registryTransactionManager") PlatformTransactionManager transactionManager,
                               DumpResourceReader reader,
                               DumpResourceWriterStep writer) {
            return new StepBuilder("resourcesDumpChunkStep", jobRepository)
                    .<Resource, Resource>chunk(10, transactionManager)
                    .reader(reader)
                    .writer(writer)
                    .build();
        }
    }

    @BeforeEach
    void setUpRequestContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void batchBeans_are_available_in_context() {
        Assertions.assertNotNull(jobRepository);
        Assertions.assertEquals("dump", dumpJob.getName());
        Assertions.assertEquals("restore", restoreJob.getName());
    }

    @Test
    void jobLauncher_is_resolvable_with_request_scope() {
        Assertions.assertNotNull(jobLauncher);
    }

    @Test
    void dumpJob_executes_and_writes_expected_files() throws Exception {
        JobExecution execution = jobLauncher.run(dumpJob, new JobParametersBuilder()
                .addString("resourceTypes", "employee")
                .addString("save", "true")
                .addString("raw", "false")
                .addString("versions", "false")
                .addLong("timestamp", System.nanoTime())
                .toJobParameters());

        Assertions.assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        Path directory = Path.of(execution.getExecutionContext().getString("directory"));
        try {
            Path employeeDirectory = directory.resolve("employee");
            Assertions.assertTrue(Files.exists(employeeDirectory.resolve("schema.json")));
            Assertions.assertTrue(execution.getExecutionContext().containsKey("addedResourceTypes"));
            Assertions.assertTrue(((List<?>) execution.getExecutionContext().get("addedResourceTypes")).contains("employee"));
        } finally {
            FileSystemUtils.deleteRecursively(directory);
        }
    }

    @Test
    void restoreJob_executes_and_persists_restored_data() throws Exception {
        Path resourceTypeDir = Files.createTempDirectory("restore-job-test").resolve(TEST_RESOURCE_TYPE);
        Files.createDirectories(resourceTypeDir);
        Files.writeString(resourceTypeDir.resolve("schema.json"), objectMapper.writeValueAsString(restoreResourceType()));
        Files.writeString(resourceTypeDir.resolve(TEST_RESOURCE_ID + ".json"), objectMapper.writeValueAsString(restoreResource()));

        try {
            JobExecution execution = jobLauncher.run(restoreJob, new JobParametersBuilder()
                    .addString("resourceType", TEST_RESOURCE_TYPE)
                    .addString("resourceTypeDir", resourceTypeDir.toString())
                    .addDate("date", new java.util.Date())
                    .toJobParameters());

            Assertions.assertEquals(BatchStatus.COMPLETED, execution.getStatus());
            Assertions.assertNotNull(resourceTypeDao.getResourceType(TEST_RESOURCE_TYPE));
            Assertions.assertNotNull(resourceDao.getResource(TEST_RESOURCE_ID));
        } finally {
            FileSystemUtils.deleteRecursively(resourceTypeDir.getParent());
        }
    }

    @Test
    void restoreJob_reads_legacy_date_timestamps_as_epoch_millis() throws Exception {
        Path resourceTypeDir = Files.createTempDirectory("restore-job-legacy-date-test").resolve(TEST_RESOURCE_TYPE);
        Files.createDirectories(resourceTypeDir);
        Files.writeString(resourceTypeDir.resolve("schema.json"), objectMapper.writeValueAsString(restoreResourceType()));
        Files.writeString(resourceTypeDir.resolve(LEGACY_DATE_RESOURCE_ID + ".json"), legacyDateResourceJson());

        try {
            JobExecution execution = jobLauncher.run(restoreJob, new JobParametersBuilder()
                    .addString("resourceType", TEST_RESOURCE_TYPE)
                    .addString("resourceTypeDir", resourceTypeDir.toString())
                    .addDate("date", new java.util.Date())
                    .toJobParameters());

            Assertions.assertEquals(BatchStatus.COMPLETED, execution.getStatus());
            Resource restored = resourceDao.getResource(LEGACY_DATE_RESOURCE_ID);
            Assertions.assertNotNull(restored);
            Assertions.assertEquals(Instant.parse("2026-04-02T10:15:30Z"), restored.getCreationDate());
            Assertions.assertEquals(Instant.parse("2026-04-03T10:15:30Z"), restored.getModificationDate());
        } finally {
            FileSystemUtils.deleteRecursively(resourceTypeDir.getParent());
        }
    }

    @Test
    void restoreJob_replaces_existing_resource_type_without_stale_delete_failure() throws Exception {
        ResourceType existing = resourceTypeDao.getResourceType("employee");
        Assertions.assertNotNull(existing);

        Path resourceTypeDir = Files.createTempDirectory("restore-job-existing-test").resolve(existing.getName());
        Files.createDirectories(resourceTypeDir);
        Files.writeString(resourceTypeDir.resolve("schema.json"), objectMapper.writeValueAsString(restoreResourceType(existing.getName(), Set.of("restoredEmployeeAlias"))));

        try {
            JobExecution execution = jobLauncher.run(restoreJob, new JobParametersBuilder()
                    .addString("resourceType", existing.getName())
                    .addString("resourceTypeDir", resourceTypeDir.toString())
                    .addDate("date", new java.util.Date())
                    .toJobParameters());

            Assertions.assertEquals(BatchStatus.COMPLETED, execution.getStatus());
            ResourceType restored = resourceTypeDao.getResourceType(existing.getName());
            Assertions.assertNotNull(restored);
            Assertions.assertEquals(Set.of("restoredEmployeeAlias"), restored.getAliases());
            Assertions.assertEquals(existing.getIndexFields().size(), restored.getIndexFields().size());
        } finally {
            FileSystemUtils.deleteRecursively(resourceTypeDir.getParent());
        }
    }

    @Test
    void dumpAndRestoreJob_preserve_all_resources_across_partitioned_ranges() throws Exception {
        ResourceType employeeType = resourceTypeDao.getResourceType("employee");
        Assertions.assertNotNull(employeeType);

        int generatedResources = 120;
        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.addAll(resourceDao.getResource(employeeType).stream().map(Resource::getId).collect(Collectors.toCollection(LinkedHashSet::new)));

        for (int i = 0; i < generatedResources; i++) {
            Resource added = resourceService.addResource(createEmployeeResource(i));
            expectedIds.add(added.getId());
        }

        JobExecution dumpExecution = jobLauncher.run(dumpJob, new JobParametersBuilder()
                .addString("resourceTypes", "employee")
                .addString("save", "true")
                .addString("raw", "false")
                .addString("versions", "false")
                .addLong("timestamp", System.nanoTime())
                .toJobParameters());

        Assertions.assertEquals(BatchStatus.COMPLETED, dumpExecution.getStatus());

        Path directory = Path.of(dumpExecution.getExecutionContext().getString("directory"));
        try {
            Path employeeDirectory = directory.resolve("employee");
            Set<String> dumpedIds;
            try (Stream<Path> dumpedFiles = Files.list(employeeDirectory)) {
                dumpedIds = dumpedFiles
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> !"schema.json".equals(name))
                        .map(name -> name.substring(0, name.length() - ".json".length()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }

            Assertions.assertEquals(expectedIds, dumpedIds);

            JobExecution restoreExecution = jobLauncher.run(restoreJob, new JobParametersBuilder()
                    .addString("resourceType", employeeType.getName())
                    .addString("resourceTypeDir", employeeDirectory.toString())
                    .addDate("date", new java.util.Date())
                    .toJobParameters());

            Assertions.assertEquals(BatchStatus.COMPLETED, restoreExecution.getStatus());

            Set<String> restoredIds = resourceDao.getResource(resourceTypeDao.getResourceType(employeeType.getName())).stream()
                    .map(Resource::getId)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Assertions.assertEquals(expectedIds.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new)), restoredIds);
        } finally {
            FileSystemUtils.deleteRecursively(directory);
        }
    }

    private ResourceType restoreResourceType() {
        return restoreResourceType(TEST_RESOURCE_TYPE, Set.of("restoreTypes"));
    }

    private ResourceType restoreResourceType(String name, Set<String> aliases) {
        ResourceType seeded = resourceTypeDao.getResourceType("employee");
        ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        resourceType.setPayloadType(seeded.getPayloadType());
        resourceType.setSchema(seeded.getSchema());
        resourceType.setSchemaUrl("not_set");
        resourceType.setIndexMapperClass(seeded.getIndexMapperClass());
        resourceType.setAliases(aliases);

        List<IndexField> fields = new ArrayList<>();
        for (IndexField seededField : seeded.getIndexFields()) {
            IndexField field = new IndexField();
            field.setName(seededField.getName());
            field.setPath(seededField.getPath());
            field.setType(seededField.getType());
            field.setLabel(seededField.getLabel());
            field.setDefaultValue(seededField.getDefaultValue());
            field.setMultivalued(seededField.isMultivalued());
            field.setPrimaryKey(seededField.isPrimaryKey());
            field.setEmbeddingWeight(seededField.getEmbeddingWeight());
            field.setRelatedResourceType(seededField.getRelatedResourceType());
            field.setRelatedResourceTypeField(seededField.getRelatedResourceTypeField());
            field.setResourceType(resourceType);
            fields.add(field);
        }
        resourceType.setIndexFields(fields);
        return resourceType;
    }

    private Resource restoreResource() {
        Resource resource = new Resource();
        resource.setId(TEST_RESOURCE_ID);
        resource.setPayloadFormat("xml");
        resource.setPayload("""
                <?xml version="1.0"?>
                <employee>
                  <author>Restored Person</author>
                  <age>35</age>
                  <single>true</single>
                  <birthday>645544821000</birthday>
                  <salary>2321.500</salary>
                  <amka>123456789012345</amka>
                </employee>
                """);
        resource.setVersion("restore-version");
        resource.setCreationDate(Instant.parse("2026-04-02T10:15:30Z"));
        resource.setModificationDate(Instant.parse("2026-04-02T10:15:30Z"));
        return resource;
    }

    private String legacyDateResourceJson() {
        return """
                {
                  "id": "%s",
                  "version": "restore-version",
                  "payload": "<?xml version=\\"1.0\\"?><employee><author>Legacy Date Person</author><age>35</age><single>true</single><birthday>645544821000</birthday><salary>2321.500</salary><amka>123456789012345</amka></employee>",
                  "payloadFormat": "xml",
                  "creationDate": 1775124930000,
                  "modificationDate": 1775211330000
                }
                """.formatted(LEGACY_DATE_RESOURCE_ID);
    }

    private Resource createEmployeeResource(int i) {
        Resource resource = new Resource();
        resource.setResourceTypeName("employee");
        resource.setPayloadFormat("xml");
        resource.setPayload("""
                <?xml version="1.0"?>
                <employee>
                  <author>Partitioned Person %d</author>
                  <age>%d</age>
                  <single>%s</single>
                  <birthday>%d</birthday>
                  <salary>%s</salary>
                  <amka>%d</amka>
                </employee>
                """.formatted(
                i,
                20 + (i % 40),
                i % 2 == 0,
                645544821000L + i,
                String.format(java.util.Locale.ROOT, "%.3f", 1000.0 + i),
                123456789000000L + i
        ));
        return resource;
    }
}
