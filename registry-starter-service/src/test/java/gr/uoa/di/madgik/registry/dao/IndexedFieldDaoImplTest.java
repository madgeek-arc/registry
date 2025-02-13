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
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_MISSING_RESOURCE_ID;
import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_RESOURCE_ID;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
class IndexedFieldDaoImplTest {

    @Autowired
    IndexedFieldDao indexedFieldDao;

    @Autowired
    ResourceDao resourceDao;


    @Test
    void getIndexedFields_OK() {
        Resource testingResource = resourceDao.getResource(TEST_RESOURCE_ID);
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(testingResource);
        Assertions.assertEquals(indexedFields.size(), 6);
    }

    @Test
    void getIndexedFields_NONE() {
        Resource testingResource = resourceDao.getResource(TEST_MISSING_RESOURCE_ID);
        Assertions.assertThrows(NullPointerException.class, () -> indexedFieldDao.getIndexedFieldsOfResource(testingResource));
    }

    @Test
    void deleteIndexedField_OK() {
        Resource testingResource = resourceDao.getResource(TEST_RESOURCE_ID);
        indexedFieldDao.deleteAllIndexedFields(testingResource);
        Assertions.assertEquals(indexedFieldDao.getIndexedFieldsOfResource(testingResource).size(), 0);
    }


}
