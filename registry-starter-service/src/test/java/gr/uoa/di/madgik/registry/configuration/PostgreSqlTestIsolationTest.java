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
import gr.uoa.di.madgik.registry.domain.Resource;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_RESOURCE_ID;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class PostgreSqlTestIsolationTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    private ResourceDao resourceDao;

    @Test
    @Order(1)
    void testData_can_be_mutated_within_a_test() {
        Resource resource = resourceDao.getResource(TEST_RESOURCE_ID);
        resourceDao.deleteResource(resource);

        Assertions.assertEquals(0, resourceDao.getResource().size());
    }

    @Test
    @Order(2)
    void testData_is_reseeded_before_the_next_test_method() {
        Resource resource = resourceDao.getResource(TEST_RESOURCE_ID);

        Assertions.assertNotNull(resource);
        Assertions.assertEquals(1, resourceDao.getResource().size());
    }
}
