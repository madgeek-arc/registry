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

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceTypeServiceTest {

    @Test
    void getIndexFieldLabels_collapses_duplicate_field_names_from_multiple_resource_types() {
        ResourceTypeService service = new TestResourceTypeService(Set.of(
                indexField("b-type", "node", "Duplicate Node"),
                indexField("a-type", "node", "Node"),
                indexField("a-type", "title", "Title")
        ));

        Map<String, String> labels = service.getIndexFieldLabels("resourceTypes");

        assertThat(labels).containsExactly(
                Map.entry("node", "Node"),
                Map.entry("title", "Title")
        );
    }

    private static IndexField indexField(String resourceTypeName, String name, String label) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(resourceTypeName);

        IndexField indexField = new IndexField();
        indexField.setResourceType(resourceType);
        indexField.setName(name);
        indexField.setLabel(label);
        return indexField;
    }

    private static class TestResourceTypeService implements ResourceTypeService {

        private final Set<IndexField> indexFields;

        private TestResourceTypeService(Set<IndexField> indexFields) {
            this.indexFields = new LinkedHashSet<>(indexFields);
        }

        @Override
        public Schema getSchema(String id) {
            return null;
        }

        @Override
        public ResourceType getResourceType(String name) {
            return null;
        }

        @Override
        public List<ResourceType> getAllResourceType() {
            return List.of();
        }

        @Override
        public List<ResourceType> getAllResourceTypeByAlias(String alias) {
            return List.of();
        }

        @Override
        public List<ResourceType> getAllResourceType(int from, int to) {
            return List.of();
        }

        @Override
        public ResourceType addResourceType(ResourceType resourceType) {
            return null;
        }

        @Override
        public ResourceType updateResourceType(ResourceType resourceType) {
            return null;
        }

        @Override
        public Set<IndexField> getResourceTypeIndexFields(String name) {
            return indexFields;
        }

        @Override
        public void deleteResourceType(String name) {
        }
    }
}
