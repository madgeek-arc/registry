/*
 * Copyright 2026-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compares two {@link ResourceType} instances for update-relevant equivalence.
 *
 * <p>This detector is intentionally narrower than entity equality. It answers the operational
 * question "would applying this update change the persisted resource type definition?" and is used
 * to skip unnecessary writes, projection refreshes, and listener side effects for no-op updates.</p>
 *
 * <p>The comparison is performed on the normalized definition surface of a resource type:
 * schema, schemaUrl, payload type, index mapper class, aliases, properties, and the full effective
 * {@link IndexField} definitions. Audit metadata and timestamps are deliberately excluded because
 * they are side effects of an update, not part of the definition being compared.</p>
 */
public final class ResourceTypeChangeDetector {

    private ResourceTypeChangeDetector() {
    }

    /**
     * Returns {@code true} when {@code candidate} would not materially change the persisted
     * definition of {@code existing}.
     *
     * <p>Aliases and properties are compared as normalized sets/maps so {@code null} and empty
     * collections are treated consistently. Index fields are compared by name and by the full field
     * definition relevant to persistence and indexing, not merely by field identity.</p>
     *
     * @param existing the currently persisted resource type definition
     * @param candidate the incoming resource type definition proposed for update
     * @return {@code true} if the update is effectively a no-op; {@code false} otherwise
     */
    public static boolean hasSameDefinition(ResourceType existing, ResourceType candidate) {
        return Objects.equals(existing.getSchema(), candidate.getSchema())
                && Objects.equals(existing.getSchemaUrl(), candidate.getSchemaUrl())
                && Objects.equals(existing.getPayloadType(), candidate.getPayloadType())
                && Objects.equals(existing.getIndexMapperClass(), candidate.getIndexMapperClass())
                && Objects.equals(normalizeAliases(existing.getAliases()), normalizeAliases(candidate.getAliases()))
                && Objects.equals(normalizeProperties(existing.getProperties()), normalizeProperties(candidate.getProperties()))
                && haveEquivalentIndexFields(existing.getIndexFields(), candidate.getIndexFields());
    }

    private static Set<String> normalizeAliases(Set<String> aliases) {
        return aliases == null ? new HashSet<>() : new HashSet<>(aliases);
    }

    private static Map<String, String> normalizeProperties(Map<String, String> properties) {
        return properties == null ? new HashMap<>() : new HashMap<>(properties);
    }

    private static boolean haveEquivalentIndexFields(List<IndexField> existingFields, List<IndexField> candidateFields) {
        if (existingFields == null || candidateFields == null) {
            return existingFields == candidateFields;
        }
        if (existingFields.size() != candidateFields.size()) {
            return false;
        }

        Map<String, IndexField> existingByName = new HashMap<>();
        for (IndexField existingField : existingFields) {
            existingByName.put(existingField.getName(), existingField);
        }

        for (IndexField candidateField : candidateFields) {
            IndexField existingField = existingByName.get(candidateField.getName());
            if (existingField == null || !areEquivalentIndexFields(existingField, candidateField)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areEquivalentIndexFields(IndexField existingField, IndexField candidateField) {
        return Objects.equals(existingField.getName(), candidateField.getName())
                && Objects.equals(existingField.getPath(), candidateField.getPath())
                && Objects.equals(existingField.getType(), candidateField.getType())
                && Objects.equals(existingField.getLabel(), candidateField.getLabel())
                && Objects.equals(existingField.getDefaultValue(), candidateField.getDefaultValue())
                && existingField.isMultivalued() == candidateField.isMultivalued()
                && existingField.isPrimaryKey() == candidateField.isPrimaryKey()
                && Objects.equals(existingField.getSearchCapabilities(), candidateField.getSearchCapabilities())
                && Float.compare(existingField.getEmbeddingWeight(), candidateField.getEmbeddingWeight()) == 0
                && Objects.equals(existingField.getRelatedResourceType(), candidateField.getRelatedResourceType())
                && Objects.equals(existingField.getRelatedResourceTypeField(), candidateField.getRelatedResourceTypeField());
    }
}
