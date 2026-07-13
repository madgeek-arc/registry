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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.dao.IndexedFieldDao;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.dao.SchemaDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.domain.index.IntegerIndexedField;
import gr.uoa.di.madgik.registry.domain.index.StringIndexedField;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_MISSING_RESOURCE_ID;
import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_RESOURCE_ID;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceServiceImplTest extends PostgreSqlTestContainerSupport {

    private static final String TEST_ACTOR = "resource-service-test";

    @MockitoBean
    EmbeddingModel embeddingModel;

    @MockitoBean
    AuditActorProvider auditActorProvider;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    @Autowired
    private IndexedFieldDao indexedFieldDao;

    @Autowired
    private SchemaDao schemaDao;

    private Resource testingResource;

    private ResourceType testingResourceType;

    @BeforeEach
    void initialize() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        testingResource = resourceService.getResource(TEST_RESOURCE_ID);
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }

    @Test
    @Order(1)
    void getResource_OK() {
        Resource resource = resourceService.getResource(TEST_RESOURCE_ID);
        Assertions.assertEquals(resource, testingResource);
        Assertions.assertEquals("legacy", resource.getCreatedBy());
        Assertions.assertEquals("legacy", resource.getModifiedBy());
    }

    @Test
    @Order(2)
    void getResource_WRONG_ID() {
        Resource resource = resourceService.getResource(TEST_MISSING_RESOURCE_ID);
        Assertions.assertNotEquals(resource, testingResource);
    }

    @Test
    @Order(3)
    void getResourceByResourceType_OK() {
        Assertions.assertNotEquals(resourceService.getResource(testingResourceType).size(), 0);
    }

    @Test
    @Order(4)
    void getResourceByResourceType_NO_RESOURCETYPE_FOUND() {
        ResourceType nullResourceType = resourceTypeDao.getResourceType("missing");
        Assertions.assertThrows(NullPointerException.class, () -> resourceService.getResource(nullResourceType).size());
    }

    @Test
    @Order(5)
    void getResourceByResourceTypeFromTo_OK() {
        Assertions.assertNotEquals(resourceService.getResource(testingResourceType, 0, 10).size(), 0);
    }

    @Test
    @Order(6)
    void getResourceByResourceTypeFromTo_OUT_OF_RANGE() {
        Assertions.assertEquals(resourceService.getResource(testingResourceType, 2, 10).size(), 0);
    }

    @Test
    @Order(7)
    void getResourcesFromTo_OK() {
        Assertions.assertNotEquals(resourceService.getResource(0, 10).size(), 0);
    }

    @Test
    @Order(8)
    void getResourcesFromTo_OUT_OF_RANGE() {
        Assertions.assertEquals(resourceService.getResource(2, 10).size(), 0);
    }

    @Test
    @Order(9)
    void addResource_OK() {

        Resource resource = newEmployeeResource("Jomazor", 28);
        resource.setResourceTypeName("employee");
        resource.setPayloadFormat("xml");

        Resource created = resourceService.addResource(resource);
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(created);

        Assertions.assertEquals(resourceService.getResource().size(), 2);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
        Assertions.assertEquals(TEST_ACTOR, created.getCreatedBy());
        Assertions.assertEquals(TEST_ACTOR, created.getModifiedBy());
        Assertions.assertEquals(6, indexedFields.size());

    }

    @Test
    @Order(10)
    void addResource_NO_RESOURCETYPE() {

        Resource resource = new Resource();
        resource.setPayload("<?xml version=\"1.0\"?> " +
                "<employee> " +
                " <author>Rallis & Polyxronopoulos</author> " +
                " <age>28</age> " +
                " <single>false</single>" +
                " <birthday>645544821000</birthday>" +
                " <salary>1292.123</salary>" +
                " <amka>051417010293821</amka>" +
                "</employee>");
        resource.setPayloadFormat("xml");

        Assertions.assertThrows(ServiceException.class, () -> resourceService.addResource(resource));
    }

    @Test
    @Order(11)
    void updateResource_OK() {
        String previousVersion = testingResource.getVersion();
        testingResource.setPayload(newEmployeePayload("Makis Dimakis", 31));
        Resource updated = resourceService.updateResource(testingResource);

        Resource resource = resourceService.getResource(TEST_RESOURCE_ID);
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(resource);

        Assertions.assertEquals(resource.getPayload(), testingResource.getPayload());
        Assertions.assertNotEquals(previousVersion, updated.getVersion());
        Assertions.assertEquals("legacy", updated.getCreatedBy());
        Assertions.assertEquals(TEST_ACTOR, updated.getModifiedBy());
        Assertions.assertEquals(6, indexedFields.size());
        Assertions.assertTrue(getIndexedField(indexedFields, "first_name", StringIndexedField.class).getValues().contains("Makis Dimakis"));
        Assertions.assertTrue(getIndexedField(indexedFields, "age", IntegerIndexedField.class).getValues().contains(31L));
    }

    @Test
    @Order(12)
    void updateResource_NOOP_SKIPS_VERSION() {
        Resource before = resourceService.getResource(TEST_RESOURCE_ID);
        String previousVersion = before.getVersion();

        // A different actor for this call only: if the no-op guard failed to skip the update body,
        // modifiedBy would flip to this value instead of staying whatever it already was.
        when(auditActorProvider.currentActor()).thenReturn("should-not-be-used");

        // A fresh, detached Resource carrying exactly what's already persisted - mirrors what
        // ResourceController.updateResource() receives from a request body.
        Resource resubmitted = new Resource();
        resubmitted.setId(before.getId());
        resubmitted.setPayload(before.getPayload());
        resubmitted.setPayloadFormat(before.getPayloadFormat());
        resubmitted.setResourceTypeName(before.getResourceTypeName());

        Resource result = resourceService.updateResource(resubmitted);

        Assertions.assertEquals(previousVersion, result.getVersion());
        Assertions.assertEquals(before.getModificationDate(), result.getModificationDate());
        Assertions.assertEquals(before.getModifiedBy(), result.getModifiedBy());
    }

    @Test
    @Order(13)
    void updateResource_MISSING_ID() {
        Resource missing = newEmployeeResource("Ghost Employee", 22);
        missing.setId("missing-resource");
        missing.setPayloadFormat("xml");
        missing.setResourceType(testingResourceType);

        ServiceException exception = Assertions.assertThrows(ServiceException.class, () -> resourceService.updateResource(missing));
        Assertions.assertEquals("Resource not found", exception.getMessage());
    }

    @Test
    @Order(14)
    void changeResourceType_OK() {
        String resourceTypeName = "employee";
        Resource resource = resourceService.changeResourceType(testingResource, resourceTypeDao.getResourceType(resourceTypeName));
        Assertions.assertEquals(resource.getResourceTypeName(), resourceTypeName);
    }

    @Test
    @Order(15)
    void changeResourceType_REGENERATES_INDEXED_FIELDS() {
        ResourceType minimalResourceType = createResourceType(
                "employee-minimal",
                testingResourceType.getSchema(),
                List.of(indexField("author_only", "//*[local-name()='author']/text()", "java.lang.String", true))
        );
        schemaDao.addSchema(schema("employee-minimal", testingResourceType.getSchema()));
        resourceTypeDao.addResourceType(minimalResourceType);

        Resource changed = resourceService.changeResourceType(testingResource, minimalResourceType);
        Resource reloaded = resourceService.getResource(changed.getId());
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(reloaded);

        Assertions.assertEquals("employee-minimal", reloaded.getResourceTypeName());
        Assertions.assertEquals(TEST_ACTOR, reloaded.getModifiedBy());
        Assertions.assertEquals(1, indexedFields.size());
        Assertions.assertTrue(getIndexedField(indexedFields, "author_only", StringIndexedField.class).getValues().contains("Jodeee"));
    }

    @Test
    @Order(16)
    void changeResourceType_INVALID_TARGET() {
        ResourceType invalidResourceType = createResourceType(
                "employee-json",
                "{}",
                List.of(indexField("author_only", "$.author", "java.lang.String", true))
        );
        invalidResourceType.setPayloadType("json");
        invalidResourceType.setIndexMapperClass(testingResourceType.getIndexMapperClass());
        resourceTypeDao.addResourceType(invalidResourceType);

        Assertions.assertThrows(ServiceException.class, () -> resourceService.changeResourceType(testingResource, invalidResourceType));
    }

    @Test
    @Order(17)
    void deleteResource() {
        resourceService.deleteResource(TEST_RESOURCE_ID);
        Assertions.assertEquals(resourceService.getResource().size(), 0);
    }

    private Resource newEmployeeResource(String author, int age) {
        Resource resource = new Resource();
        resource.setPayload(newEmployeePayload(author, age));
        return resource;
    }

    private String newEmployeePayload(String author, int age) {
        return "<?xml version=\"1.0\"?> " +
                "<employee> " +
                " <author>" + author + "</author> " +
                " <age>" + age + "</age> " +
                " <single>false</single>" +
                " <birthday>645544821000</birthday>" +
                " <salary>1292.123</salary>" +
                " <amka>051417010293821</amka>" +
                "</employee>";
    }

    private ResourceType createResourceType(String name, String schema, List<IndexField> indexFields) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        resourceType.setSchema(schema);
        resourceType.setSchemaUrl("not_set");
        resourceType.setPayloadType("xml");
        resourceType.setIndexMapperClass(testingResourceType.getIndexMapperClass());
        indexFields.forEach(indexField -> indexField.setResourceType(resourceType));
        resourceType.setIndexFields(indexFields);
        return resourceType;
    }

    private IndexField indexField(String name, String path, String type, boolean primaryKey) {
        IndexField indexField = new IndexField();
        indexField.setName(name);
        indexField.setLabel(name);
        indexField.setPath(path);
        indexField.setType(type);
        indexField.setPrimaryKey(primaryKey);
        indexField.setMultivalued(false);
        return indexField;
    }

    private <T extends IndexedField<?>> T getIndexedField(List<IndexedField> indexedFields, String name, Class<T> type) {
        return indexedFields.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .filter(indexedField -> name.equals(indexedField.getName()))
                .findFirst()
                .orElseThrow();
    }

    private Schema schema(String originalUrl, String value) {
        Schema schema = new Schema();
        schema.setId(originalUrl + "-schema");
        schema.setOriginalUrl(originalUrl);
        schema.setSchema(value);
        return schema;
    }

}
