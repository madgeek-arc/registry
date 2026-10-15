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
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class ReservedWordColumnSearchTest extends PostgreSqlTestContainerSupport {

    private static final String TYPE = "reserved_word_type";

    @MockitoBean
    EmbeddingService embeddingService;

    @MockitoBean
    AuditActorProvider auditActorProvider;

    @Autowired
    SearchService searchService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    ViewService viewService;

    @BeforeAll
    void createTypeWithReservedWordColumn() {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(TYPE);
        resourceType.setSchema("{}");
        resourceType.setPayloadType("json");
        resourceType.setIndexMapperClass("gr.uoa.di.madgik.registry.index.DefaultIndexMapper");
        List<IndexField> fields = new ArrayList<>();
        fields.add(indexField("resource_internal_id", "$.id", "java.lang.String", false, true));
        fields.add(indexField("order", "$.order", "java.lang.String", false, false));
        fields.add(indexField("Tags", "$.tags[*]", "java.lang.String", true, false));
        fields.forEach(f -> f.setResourceType(resourceType));
        resourceType.setIndexFields(fields);
        resourceTypeService.addResourceType(resourceType);
    }

    @BeforeEach
    void stubActor() {
        lenient().when(auditActorProvider.currentActor()).thenReturn("search-test");
    }

    @Test
    void view_isCreatedWithReservedWordColumn() {
        assertDoesNotThrow(() -> viewService.createView(resourceTypeService.getResourceType(TYPE)));
    }

    @Test
    void filter_onReservedWordColumn() {
        FacetFilter filter = filter();
        filter.addFilter("order", "first");

        assertEquals(0, searchService.search(filter).getTotal());
    }

    @Test
    void filter_onMixedCaseColumn_ignoresCase() {
        FacetFilter filter = filter();
        filter.addFilter("tags", List.of("a", "b"));

        assertEquals(0, searchService.search(filter).getTotal());
    }

    @Test
    void orderBy_onReservedWordColumnDoesNotFail() {
        FacetFilter filter = filter();
        filter.addOrderBy("creation_date", "desc");

        assertDoesNotThrow(() -> searchService.search(filter));
    }

    @Test
    void cql_onReservedWordColumn() {
        Paging<Resource> result = searchService.cqlQuery("order = \"first\"", TYPE);

        assertEquals(0, result.getTotal());
    }

    private FacetFilter filter() {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(TYPE);
        filter.setQuantity(10);
        return filter;
    }

    private IndexField indexField(String name, String path, String type, boolean multivalued, boolean primaryKey) {
        IndexField indexField = new IndexField();
        indexField.setName(name);
        indexField.setLabel(name);
        indexField.setPath(path);
        indexField.setType(type);
        indexField.setMultivalued(multivalued);
        indexField.setPrimaryKey(primaryKey);
        return indexField;
    }
}
