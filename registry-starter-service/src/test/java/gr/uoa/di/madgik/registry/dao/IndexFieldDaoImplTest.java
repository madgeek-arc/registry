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

package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndexFieldDaoImplTest {

    @Autowired
    IndexFieldDao indexFieldDao;

    @Autowired
    ResourceTypeDao resourceTypeDao;

    private ResourceType testingResourceType;


    @BeforeAll
    void initialize() {
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }


    @Test
    void getIndexFields_OK() {
        List<IndexField> indexFields = indexFieldDao.getIndexFieldsOfResourceType(testingResourceType);
        Assertions.assertEquals(indexFields.size(), 6);
    }

    @Test
    void getIndexFields_NONE() {
        ResourceType resourceType = resourceTypeDao.getResourceType("event");
        Assertions.assertThrows(NullPointerException.class, () -> indexFieldDao.getIndexFieldsOfResourceType(resourceType));
    }

}
