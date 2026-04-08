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
import gr.uoa.di.madgik.registry.dao.SchemaDao;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
class ResourceTypeServiceImplTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    SchemaDao schemaDao;

    @Test
    void updateResourceType_replaces_index_fields_and_updates_schema_entry() {
        ResourceType existing = resourceTypeService.getResourceType("employee");

        ResourceType updated = new ResourceType();
        updated.setName(existing.getName());
        updated.setPayloadType(existing.getPayloadType());
        updated.setSchema(existing.getSchema());
        updated.setSchemaUrl(null);
        updated.setIndexMapperClass(existing.getIndexMapperClass());
        updated.setAliases(Set.of("resourceTypes", "updatedTypes"));

        IndexField primaryKey = new IndexField();
        primaryKey.setName("employee_id");
        primaryKey.setLabel("employee_id");
        primaryKey.setPath("//*[local-name()='author']/text()");
        primaryKey.setType("java.lang.String");
        primaryKey.setPrimaryKey(true);

        IndexField salary = new IndexField();
        salary.setName("salary");
        salary.setLabel("salary");
        salary.setPath("//*[local-name()='salary']/text()");
        salary.setType("java.lang.Float");

        updated.setIndexFields(List.of(primaryKey, salary));

        ResourceType persisted = resourceTypeService.updateResourceType(updated);

        assertThat(persisted.getIndexFields())
                .extracting(IndexField::getName)
                .containsExactlyInAnyOrder("employee_id", "salary");
        assertThat(resourceTypeService.getResourceTypeIndexFields("employee"))
                .extracting(IndexField::getName)
                .containsExactlyInAnyOrder("employee_id", "salary");
        assertThat(resourceTypeService.getAllResourceTypeByAlias("updatedTypes"))
                .extracting(ResourceType::getName)
                .containsExactly("employee");
        assertThat(schemaDao.getSchemaByUrl("employee").getSchema()).isEqualTo(existing.getSchema());
    }
}
