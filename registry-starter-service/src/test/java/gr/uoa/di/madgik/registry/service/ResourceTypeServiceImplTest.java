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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
class ResourceTypeServiceImplTest extends PostgreSqlTestContainerSupport {

    private static final String TEST_ACTOR = "resource-type-test";

    @MockitoBean
    EmbeddingModel embeddingModel;

    @MockitoBean
    AuditActorProvider auditActorProvider;

    @MockitoSpyBean
    ResourceTypeProjectionRefreshService resourceTypeProjectionRefreshService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    SchemaDao schemaDao;

    @Autowired
    ViewService viewService;

    @PersistenceContext(unitName = "registryEntityManager")
    EntityManager entityManager;

    @Test
    void getResourceType_returns_seeded_audit_metadata() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);

        ResourceType existing = resourceTypeService.getResourceType("employee");

        assertThat(existing.getCreatedBy()).isEqualTo("legacy");
        assertThat(existing.getModifiedBy()).isEqualTo("legacy");
    }

    @Test
    void updateResourceType_replaces_index_fields_and_updates_schema_entry() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);

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
        assertThat(persisted.getCreatedBy()).isEqualTo("legacy");
        assertThat(persisted.getModifiedBy()).isEqualTo(TEST_ACTOR);
        assertThat(resourceTypeService.getResourceTypeIndexFields("employee"))
                .extracting(IndexField::getName)
                .containsExactlyInAnyOrder("employee_id", "salary");
        assertThat(resourceTypeService.getResourceType("employee").getModifiedBy()).isEqualTo(TEST_ACTOR);
        assertThat(resourceTypeService.getAllResourceTypeByAlias("updatedTypes"))
                .extracting(ResourceType::getName)
                .containsExactly("employee");
        assertThat(schemaDao.getSchemaByUrl("employee").getSchema()).isEqualTo(existing.getSchema());
    }

    @Test
    void updateResourceType_with_already_managed_instance_still_detects_real_change() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);

        // Mirrors a caller that fetches the current entity and mutates it in place before
        // resubmitting the same instance (e.g. GenericResourceManager.update()'s shape for
        // Resource) - the pattern that defeated a naive existing-vs-candidate comparison. See the
        // caveat on ResourceTypeChangeDetector.hasSameDefinition.
        ResourceType existing = resourceTypeService.getResourceType("employee");
        existing.getAliases().add("changed-alias");

        resourceTypeService.updateResourceType(existing);

        assertThat(resourceTypeService.getResourceType("employee").getAliases())
                .contains("changed-alias");
        verify(resourceTypeProjectionRefreshService).refresh(any());
    }

    @Test
    void updateResourceType_noop_with_already_managed_instance_skips_refresh() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);

        ResourceType existing = resourceTypeService.getResourceType("employee");

        resourceTypeService.updateResourceType(existing);

        verify(resourceTypeProjectionRefreshService, never()).refresh(any());
    }

    @Test
    void updateResourceType_recreates_view_with_updated_columns() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);

        ResourceType existing = resourceTypeService.getResourceType("employee");
        viewService.createView(existing);

        assertThat(getViewColumns("employee_view"))
                .contains("first_name", "age", "single", "birthday", "salary", "amka")
                .doesNotContain("employee_id");

        ResourceType updated = new ResourceType();
        updated.setName(existing.getName());
        updated.setPayloadType(existing.getPayloadType());
        updated.setSchema(existing.getSchema());
        updated.setSchemaUrl(null);
        updated.setIndexMapperClass(existing.getIndexMapperClass());
        updated.setAliases(existing.getAliases());
        updated.setProperties(existing.getProperties());

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

        resourceTypeService.updateResourceType(updated);

        assertThat(getViewColumns("employee_view"))
                .contains("employee_id", "salary")
                .doesNotContain("first_name", "age", "single", "birthday", "amka");
    }

    @Test
    void deleteResourceType_handles_multiple_aliases_and_properties_without_stale_index_field_delete() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);

        ResourceType existing = resourceTypeService.getResourceType("employee");

        ResourceType updated = new ResourceType();
        updated.setName(existing.getName());
        updated.setPayloadType(existing.getPayloadType());
        updated.setSchema(existing.getSchema());
        updated.setSchemaUrl(null);
        updated.setIndexMapperClass(existing.getIndexMapperClass());
        updated.setAliases(Set.of("resourceTypes", "employee-alt"));
        updated.setProperties(java.util.Map.of("source", "test", "visibility", "public"));
        updated.setIndexFields(existing.getIndexFields());

        resourceTypeService.updateResourceType(updated);

        resourceTypeService.deleteResourceType(existing.getName());

        assertThat(resourceTypeService.getResourceType(existing.getName())).isNull();
    }

    @Test
    void updateResourceType_refreshes_existing_resource_projections_when_index_fields_are_added_renamed_and_deleted() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);

        ResourceType existing = resourceTypeService.getResourceType("employee");
        viewService.createView(existing);

        assertThat(getIndexedStringFieldValues())
                .containsExactly(tuple("first_name", "Jodeee"));
        assertThat(getViewColumns("employee_view")).doesNotContain("nickname", "preferred_name");
        assertThat(getViewRowValues("employee_view", "first_name"))
                .containsExactly(tuple("Jodeee"));

        resourceTypeService.updateResourceType(copyWithIndexFields(existing, List.of(
                indexField("first_name", "//*[local-name()='author']/text()", "java.lang.String", true),
                indexField("age", "//*[local-name()='age']/text()", "java.lang.Integer", false),
                indexField("single", "//*[local-name()='single']/text()", "java.lang.Boolean", false),
                indexField("birthday", "//*[local-name()='birthday']/text()", "java.time.Instant", false),
                indexField("salary", "//*[local-name()='salary']/text()", "java.lang.Float", false),
                indexField("amka", "//*[local-name()='amka']/text()", "java.lang.Long", false),
                indexField("nickname", "//*[local-name()='author']/text()", "java.lang.String", false)
        )));

        assertThat(getIndexedStringFieldValues())
                .containsExactlyInAnyOrder(
                        tuple("first_name", "Jodeee"),
                        tuple("nickname", "Jodeee")
                );
        assertThat(getViewColumns("employee_view")).contains("nickname");
        assertThat(getViewRowValues("employee_view", "first_name", "nickname"))
                .containsExactly(tuple("Jodeee", "Jodeee"));

        resourceTypeService.updateResourceType(copyWithIndexFields(existing, List.of(
                indexField("first_name", "//*[local-name()='author']/text()", "java.lang.String", true),
                indexField("age", "//*[local-name()='age']/text()", "java.lang.Integer", false),
                indexField("single", "//*[local-name()='single']/text()", "java.lang.Boolean", false),
                indexField("birthday", "//*[local-name()='birthday']/text()", "java.time.Instant", false),
                indexField("salary", "//*[local-name()='salary']/text()", "java.lang.Float", false),
                indexField("amka", "//*[local-name()='amka']/text()", "java.lang.Long", false),
                indexField("preferred_name", "//*[local-name()='author']/text()", "java.lang.String", false)
        )));

        assertThat(getIndexedStringFieldValues())
                .containsExactlyInAnyOrder(
                        tuple("first_name", "Jodeee"),
                        tuple("preferred_name", "Jodeee")
                );
        assertThat(getIndexedStringFieldValues())
                .doesNotContain(tuple("nickname", "Jodeee"));
        assertThat(getViewColumns("employee_view"))
                .contains("preferred_name")
                .doesNotContain("nickname");
        assertThat(getViewRowValues("employee_view", "first_name", "preferred_name"))
                .containsExactly(tuple("Jodeee", "Jodeee"));

        resourceTypeService.updateResourceType(copyWithIndexFields(existing, List.of(
                indexField("first_name", "//*[local-name()='author']/text()", "java.lang.String", true),
                indexField("age", "//*[local-name()='age']/text()", "java.lang.Integer", false),
                indexField("single", "//*[local-name()='single']/text()", "java.lang.Boolean", false),
                indexField("birthday", "//*[local-name()='birthday']/text()", "java.time.Instant", false),
                indexField("salary", "//*[local-name()='salary']/text()", "java.lang.Float", false),
                indexField("amka", "//*[local-name()='amka']/text()", "java.lang.Long", false)
        )));

        assertThat(getIndexedStringFieldValues())
                .containsExactly(tuple("first_name", "Jodeee"));
        assertThat(getViewColumns("employee_view"))
                .doesNotContain("nickname", "preferred_name");
        assertThat(getViewRowValues("employee_view", "first_name"))
                .containsExactly(tuple("Jodeee"));
    }

    @Test
    void addResourceType_rejects_alias_with_mismatched_primaryKey_field_name() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        ResourceType employee = resourceTypeService.getResourceType("employee");

        ResourceType candidate = newResourceType("dept_a", employee, Set.of("resourceTypes"), List.of(
                indexField("dept_id", "//*[local-name()='id']/text()", "java.lang.String", true)));

        assertThrows(ServiceException.class, () -> resourceTypeService.addResourceType(candidate));
        assertThat(resourceTypeService.getResourceType("dept_a")).isNull();
    }

    @Test
    void addResourceType_accepts_alias_with_matching_primaryKey_field_name_ignoring_type() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        ResourceType employee = resourceTypeService.getResourceType("employee");

        ResourceType candidate = newResourceType("dept_b", employee, Set.of("resourceTypes"), List.of(
                indexField("first_name", "//*[local-name()='id']/text()", "java.lang.Integer", true)));

        resourceTypeService.addResourceType(candidate);

        assertThat(resourceTypeService.getAllResourceTypeByAlias("resourceTypes"))
                .extracting(ResourceType::getName)
                .contains("employee", "dept_b");
    }

    @Test
    void addResourceType_allows_type_with_no_aliases() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        ResourceType employee = resourceTypeService.getResourceType("employee");

        ResourceType candidate = newResourceType("dept_c", employee, Set.of(), List.of(
                indexField("anything_id", "//*[local-name()='id']/text()", "java.lang.String", true)));

        ResourceType persisted = resourceTypeService.addResourceType(candidate);

        assertThat(persisted.getName()).isEqualTo("dept_c");
    }

    @Test
    void addResourceType_allows_alias_unclaimed_by_any_other_type() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        ResourceType employee = resourceTypeService.getResourceType("employee");

        ResourceType candidate = newResourceType("dept_d", employee, Set.of("totally-unique-alias-xyz"), List.of(
                indexField("anything_id", "//*[local-name()='id']/text()", "java.lang.String", true)));

        resourceTypeService.addResourceType(candidate);

        assertThat(resourceTypeService.getAllResourceTypeByAlias("totally-unique-alias-xyz"))
                .extracting(ResourceType::getName)
                .containsExactly("dept_d");
    }

    @Test
    void updateResourceType_rejects_alias_causing_primaryKey_conflict() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        ResourceType employee = resourceTypeService.getResourceType("employee");

        ResourceType typeA = newResourceType("dept_e", employee, Set.of("dept-alias"), List.of(
                indexField("dept_id", "//*[local-name()='id']/text()", "java.lang.String", true)));
        resourceTypeService.addResourceType(typeA);

        ResourceType typeB = newResourceType("dept_f", employee, Set.of("dept-f-alias"), List.of(
                indexField("unit_id", "//*[local-name()='id']/text()", "java.lang.String", true)));
        resourceTypeService.addResourceType(typeB);

        ResourceType typeBUpdate = newResourceType("dept_f", employee, Set.of("dept-f-alias", "dept-alias"), List.of(
                indexField("unit_id", "//*[local-name()='id']/text()", "java.lang.String", true)));

        assertThrows(ServiceException.class, () -> resourceTypeService.updateResourceType(typeBUpdate));
        assertThat(resourceTypeService.getResourceType("dept_f").getAliases()).doesNotContain("dept-alias");
    }

    @Test
    void updateResourceType_accepts_alias_when_primaryKey_field_names_match() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        ResourceType employee = resourceTypeService.getResourceType("employee");

        ResourceType typeA = newResourceType("dept_i", employee, Set.of("dept-i-alias"), List.of(
                indexField("dept_id", "//*[local-name()='id']/text()", "java.lang.String", true)));
        resourceTypeService.addResourceType(typeA);

        ResourceType typeB = newResourceType("dept_j", employee, Set.of("dept-j-alias"), List.of(
                indexField("dept_id", "//*[local-name()='id']/text()", "java.lang.Integer", true)));
        resourceTypeService.addResourceType(typeB);

        ResourceType typeBUpdate = newResourceType("dept_j", employee, Set.of("dept-j-alias", "dept-i-alias"), List.of(
                indexField("dept_id", "//*[local-name()='id']/text()", "java.lang.Integer", true)));

        resourceTypeService.updateResourceType(typeBUpdate);

        assertThat(resourceTypeService.getAllResourceTypeByAlias("dept-i-alias"))
                .extracting(ResourceType::getName)
                .containsExactlyInAnyOrder("dept_i", "dept_j");
    }

    @Test
    void updateResourceType_self_exclusion_allows_renaming_own_primaryKey_field() {
        when(auditActorProvider.currentActor()).thenReturn(TEST_ACTOR);
        ResourceType employee = resourceTypeService.getResourceType("employee");

        ResourceType candidate = newResourceType("dept_h", employee, Set.of("dept-h-alias"), List.of(
                indexField("dept_id", "//*[local-name()='id']/text()", "java.lang.String", true)));
        resourceTypeService.addResourceType(candidate);

        // Renaming the sole primaryKey field while keeping the same, solely-owned alias must not
        // conflict against its own previously-persisted definition - this is exactly what
        // self-exclusion in validateAliasPrimaryKeyCompatibility guards against.
        ResourceType renamed = newResourceType("dept_h", employee, Set.of("dept-h-alias"), List.of(
                indexField("dept_code", "//*[local-name()='id']/text()", "java.lang.String", true)));

        ResourceType persisted = resourceTypeService.updateResourceType(renamed);

        assertThat(persisted.getIndexFields()).extracting(IndexField::getName).containsExactly("dept_code");
    }

    private ResourceType newResourceType(String name, ResourceType schemaSource, Set<String> aliases,
                                          List<IndexField> indexFields) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        resourceType.setPayloadType(schemaSource.getPayloadType());
        resourceType.setSchema(schemaSource.getSchema());
        resourceType.setSchemaUrl(null);
        resourceType.setIndexMapperClass(schemaSource.getIndexMapperClass());
        resourceType.setAliases(aliases);
        resourceType.setIndexFields(indexFields);
        return resourceType;
    }

    private List<String> getViewColumns(String viewName) {
        return entityManager.createNativeQuery(
                        "SELECT column_name FROM information_schema.columns " +
                                "WHERE table_name = :viewName ORDER BY ordinal_position"
                )
                .setParameter("viewName", viewName)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<org.assertj.core.groups.Tuple> getIndexedStringFieldValues() {
        List<Object[]> rows = (List<Object[]>) (List<?>) entityManager.createNativeQuery(
                        "SELECT sif.name, sifv.values " +
                                "FROM stringindexedfield sif " +
                                "JOIN stringindexedfield_values sifv ON sif.id = sifv.stringindexedfield_id " +
                                "WHERE sif.resource_id = :resourceId"
                )
                .setParameter("resourceId", DatabaseConfiguration.TEST_RESOURCE_ID)
                .getResultList();

        return rows.stream()
                .map(row -> tuple(row[0], row[1]))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<org.assertj.core.groups.Tuple> getViewRowValues(String viewName, String... columns) {
        String select = String.join(", ", columns);
        return entityManager.createNativeQuery("SELECT " + select + " FROM " + viewName)
                .getResultList()
                .stream()
                .map(row -> row instanceof Object[] values ? tuple(values) : tuple(row))
                .toList();
    }

    private ResourceType copyWithIndexFields(ResourceType existing, List<IndexField> indexFields) {
        ResourceType updated = new ResourceType();
        updated.setName(existing.getName());
        updated.setPayloadType(existing.getPayloadType());
        updated.setSchema(existing.getSchema());
        updated.setSchemaUrl(null);
        updated.setIndexMapperClass(existing.getIndexMapperClass());
        updated.setAliases(existing.getAliases());
        updated.setProperties(existing.getProperties());
        updated.setIndexFields(indexFields);
        return updated;
    }

    private IndexField indexField(String name, String path, String type, boolean primaryKey) {
        IndexField indexField = new IndexField();
        indexField.setName(name);
        indexField.setLabel(name);
        indexField.setPath(path);
        indexField.setType(type);
        indexField.setPrimaryKey(primaryKey);
        return indexField;
    }
}
