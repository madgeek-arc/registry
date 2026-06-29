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

package gr.uoa.di.madgik.registry.startup;

import tools.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ResourceTypeInitHashTest {

    private static final ResourceTypeInit INIT = new ResourceTypeInit(
            "classpath*:resourceTypes",
            false,
            mock(ResourceTypeService.class),
            new ObjectMapper()
    );

    @Test
    void sameContent_differentMetadata_sameHash() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setCreationDate(Instant.parse("2024-01-01T00:00:00Z"));
        a.setModificationDate(Instant.parse("2024-01-01T00:00:00Z"));
        a.setCreatedBy("alice");
        a.setModifiedBy("alice");

        ResourceType b = resourceType("service", "{}", "json");
        b.setCreationDate(Instant.parse("2025-06-01T12:00:00Z"));
        b.setModificationDate(Instant.parse("2025-06-01T12:00:00Z"));
        b.setCreatedBy("system");
        b.setModifiedBy("bob");

        assertThat(INIT.contentHash(a)).isEqualTo(INIT.contentHash(b));
    }

    @Test
    void differentSchema_differentHash() {
        ResourceType a = resourceType("service", "{\"v\":1}", "json");
        ResourceType b = resourceType("service", "{\"v\":2}", "json");

        assertThat(INIT.contentHash(a)).isNotEqualTo(INIT.contentHash(b));
    }

    @Test
    void differentPayloadType_differentHash() {
        ResourceType a = resourceType("service", "{}", "json");
        ResourceType b = resourceType("service", "{}", "xml");

        assertThat(INIT.contentHash(a)).isNotEqualTo(INIT.contentHash(b));
    }

    @Test
    void aliases_orderIndependent() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setAliases(new LinkedHashSet<>(List.of("beta", "alpha", "gamma")));

        ResourceType b = resourceType("service", "{}", "json");
        b.setAliases(new LinkedHashSet<>(List.of("gamma", "alpha", "beta")));

        assertThat(INIT.contentHash(a)).isEqualTo(INIT.contentHash(b));
    }

    @Test
    void differentAliases_differentHash() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setAliases(new LinkedHashSet<>(List.of("alpha")));

        ResourceType b = resourceType("service", "{}", "json");
        b.setAliases(new LinkedHashSet<>(List.of("beta")));

        assertThat(INIT.contentHash(a)).isNotEqualTo(INIT.contentHash(b));
    }

    @Test
    void nullAliases_sameAsEmpty() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setAliases(null);

        ResourceType b = resourceType("service", "{}", "json");
        b.setAliases(new LinkedHashSet<>());

        assertThat(INIT.contentHash(a)).isEqualTo(INIT.contentHash(b));
    }

    @Test
    void indexFields_orderIndependent() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setIndexFields(List.of(indexField("z-field", "string"), indexField("a-field", "string")));

        ResourceType b = resourceType("service", "{}", "json");
        b.setIndexFields(List.of(indexField("a-field", "string"), indexField("z-field", "string")));

        assertThat(INIT.contentHash(a)).isEqualTo(INIT.contentHash(b));
    }

    @Test
    void differentIndexField_differentHash() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setIndexFields(List.of(indexField("name", "string")));

        ResourceType b = resourceType("service", "{}", "json");
        b.setIndexFields(List.of(indexField("name", "integer")));

        assertThat(INIT.contentHash(a)).isNotEqualTo(INIT.contentHash(b));
    }

    @Test
    void nullIndexFields_sameAsEmpty() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setIndexFields(null);

        ResourceType b = resourceType("service", "{}", "json");
        b.setIndexFields(List.of());

        assertThat(INIT.contentHash(a)).isEqualTo(INIT.contentHash(b));
    }

    @Test
    void properties_included_in_hash() {
        ResourceType a = resourceType("service", "{}", "json");
        a.setProperties(Map.of("key", "value1"));

        ResourceType b = resourceType("service", "{}", "json");
        b.setProperties(Map.of("key", "value2"));

        assertThat(INIT.contentHash(a)).isNotEqualTo(INIT.contentHash(b));
    }

    @Test
    void indexField_searchCapabilities_included() {
        IndexField keyword = indexField("title", "java.lang.String");
        keyword.setSearchCapabilities(EnumSet.of(SearchCapability.KEYWORD));

        IndexField text = indexField("title", "java.lang.String");
        text.setSearchCapabilities(EnumSet.of(SearchCapability.TEXT));

        ResourceType a = resourceType("service", "{}", "json");
        a.setIndexFields(List.of(keyword));

        ResourceType b = resourceType("service", "{}", "json");
        b.setIndexFields(List.of(text));

        assertThat(INIT.contentHash(a)).isNotEqualTo(INIT.contentHash(b));
    }

    // --- helpers ---

    private static ResourceType resourceType(String name, String schema, String payloadType) {
        ResourceType rt = new ResourceType();
        rt.setName(name);
        rt.setSchema(schema);
        rt.setPayloadType(payloadType);
        return rt;
    }

    private static IndexField indexField(String name, String type) {
        IndexField f = new IndexField();
        f.setName(name);
        f.setType(type);
        return f;
    }
}
