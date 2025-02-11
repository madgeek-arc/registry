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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_MISSING_RESOURCE_ID;
import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_RESOURCE_ID;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceServiceImplTest {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    private Resource testingResource;

    private ResourceType testingResourceType;

    @BeforeAll
    void initialize() {
        testingResource = resourceService.getResource(TEST_RESOURCE_ID);
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }

    @Test
    @Order(1)
    void getResource_OK() {
        Resource resource = resourceService.getResource(TEST_RESOURCE_ID);
        Assertions.assertEquals(resource, testingResource);
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
        // TODO: remove expected exception when IndexedFields values column is renamed
    void addResource_OK() {

        Resource resource = new Resource();
        resource.setPayload("<?xml version=\"1.0\"?> " +
                "<employee> " +
                " <author>Jomazor</author> " +
                " <age>28</age> " +
                " <single>false</single>" +
                " <birthday>645544821000</birthday>" +
                " <salary>1292.123</salary>" +
                " <amka>051417010293821</amka>" +
                "</employee>");
        resource.setResourceTypeName("employee");
        resource.setPayloadFormat("xml");

        Assertions.assertThrows(ServiceException.class, () -> {
            resourceService.addResource(resource);
        });
        Assertions.assertEquals(resourceService.getResource().size(), 2);

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
        testingResource.setPayload("<?xml version=\"1.0\"?> " +
                "<employee> " +
                " <author>Makis Dimakis</author> " +
                " <age>28</age> " +
                " <single>false</single>" +
                " <birthday>645544821000</birthday>" +
                " <salary>1292.123</salary>" +
                " <amka>051417010293821</amka>" +
                "</employee>");
        resourceService.updateResource(testingResource);

        Resource resource = resourceService.getResource(TEST_RESOURCE_ID);

        Assertions.assertEquals(resource.getPayload(), testingResource.getPayload());

    }

    @Test
    @Order(12)
    void changeResourceType_OK() {
        String resourceTypeName = "employee";
        Resource resource = resourceService.changeResourceType(testingResource, resourceTypeDao.getResourceType(resourceTypeName));
        Assertions.assertEquals(resource.getResourceTypeName(), resourceTypeName);
    }

    @Test
    @Order(13)
    void deleteResource() {
        resourceService.deleteResource(TEST_RESOURCE_ID);
        Assertions.assertEquals(resourceService.getResource().size(), 0);
    }

}
