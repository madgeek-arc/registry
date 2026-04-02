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

package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceTypeDaoImplTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    ResourceTypeDao resourceTypeDao;

    private ResourceType testingResourceType;


    @BeforeEach
    void initialize() {
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }

    @Test
    @Order(1)
    void getResourceTypeIndexFields_OK() {
        Assertions.assertEquals(resourceTypeDao.getResourceTypeIndexFields("employee").size(), testingResourceType.getIndexFields().size());
    }

    @Test
    @Order(2)
    void getAllResourceTypeByAlias_OK() {
        Assertions.assertEquals(1, resourceTypeDao.getAllResourceTypeByAlias("resourceTypes").size());
        Assertions.assertEquals("employee", resourceTypeDao.getAllResourceTypeByAlias("resourceTypes").getFirst().getName());
    }

    @Test
    @Order(3)
    void getResourceTypeIndexFields_NOT_FOUND() {
        Assertions.assertNotEquals(resourceTypeDao.getResourceTypeIndexFields("event").toArray(), testingResourceType.getIndexFields().toArray());
    }

    @Test
    @Order(4)
    void deleteResourceType() {
        resourceTypeDao.deleteResourceType(testingResourceType.getName());
        Assertions.assertNull(resourceTypeDao.getResourceType("employee"));
    }


}
