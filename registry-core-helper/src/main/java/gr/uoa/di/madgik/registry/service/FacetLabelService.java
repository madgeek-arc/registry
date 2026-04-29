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

import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.Value;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enriches {@link Value#getLabel()} for facets returned by a search query.
 *
 * <p>Facet values are raw indexed-field values (typically IDs referencing resources of another
 * {@link gr.uoa.di.madgik.registry.domain.ResourceType}). This service resolves human-readable
 * labels for those IDs by performing a single batch lookup per distinct referenced resource type.
 *
 * <h2>Configuration</h2>
 * <p>Label enrichment for a facet field is opt-in and driven by two nullable properties on
 * {@link IndexField}:
 * <ul>
 *   <li>{@code relatedResourceType} — the name of the {@code ResourceType} whose resources'
 *       primary-key values appear as facet values for this field. If {@code null}, no enrichment
 *       is performed for that facet.</li>
 *   <li>{@code relatedResourceTypeField} — the {@code IndexField} name in the related
 *       {@code ResourceType} that holds the display label. Falls back to a field named
 *       {@code "name"} in the related type. If neither is found, the facet value itself is
 *       converted to proper case (e.g. {@code "my_value"} → {@code "My Value"}).</li>
 * </ul>
 *
 * <h2>Performance</h2>
 * <p>At most one {@link SearchService#getLabels} call is made per distinct
 * {@code relatedResourceType} referenced by the supplied facets, regardless of how many values
 * each facet has.
 *
 * <h2>Wiring</h2>
 * <p>Inject this bean and call {@link #enrichFacetLabels(List, String)} after obtaining a
 * {@code Paging} result from {@link SearchService#search}:
 * <pre>{@code
 * Paging<Resource> result = searchService.search(filter);
 * facetLabelService.enrichFacetLabels(result.getFacets(), filter.getResourceType());
 * }</pre>
 * Alternatively, downstream applications may wrap the call transparently using an AOP
 * {@code @AfterReturning} aspect.
 */
@Service
public class FacetLabelService {

    private static final Logger logger = LoggerFactory.getLogger(FacetLabelService.class);

    private final SearchService searchService;
    private final ResourceTypeService resourceTypeService;

    public FacetLabelService(SearchService searchService,
                             ResourceTypeService resourceTypeService) {
        this.searchService = searchService;
        this.resourceTypeService = resourceTypeService;
    }

    /**
     * Enriches {@link Value#getLabel()} for each facet in the list whose backing
     * {@link IndexField} has {@code relatedResourceType} configured.
     *
     * <p>Facets whose {@code IndexField} has no {@code relatedResourceType} are silently skipped.
     * Values for which no matching resource is found receive a proper-cased version of the raw
     * value as a fallback label (e.g. {@code "my_value"} → {@code "My Value"}).
     *
     * @param facets           the facets to enrich, typically from {@code Paging.getFacets()};
     *                         must not be {@code null}
     * @param resourceTypeName the name of the {@code ResourceType} that was searched;
     *                         used to resolve {@code IndexField} configuration
     */
    public void enrichFacetLabels(List<Facet> facets, String resourceTypeName) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        Map<String, IndexField> fieldMap = buildFieldMap(resourceTypeName);

        // Group facets by their relatedResourceType so we issue one getLabels() call per type.
        Map<String, List<Facet>> byRelatedType = new LinkedHashMap<>();
        for (Facet facet : facets) {
            IndexField field = fieldMap.get(facet.getField());
            if (field == null) {
                continue;
            }
            if (!field.hasSearchCapability(SearchCapability.KEYWORD)) {
                continue;
            }
            if (field.getRelatedResourceType() != null) {
                byRelatedType
                        .computeIfAbsent(field.getRelatedResourceType(), k -> new ArrayList<>())
                        .add(facet);
            }
        }

        for (Map.Entry<String, List<Facet>> entry : byRelatedType.entrySet()) {
            String relatedType       = entry.getKey();
            List<Facet> relatedFacets = entry.getValue();

            String primaryKeyField = resolvePrimaryKeyField(relatedType);
            if (primaryKeyField == null) {
                logger.warn("No primaryKey IndexField found for resource type '{}'; " +
                        "skipping label enrichment for facets: {}",
                        relatedType,
                        relatedFacets.stream().map(Facet::getField).collect(Collectors.joining(", ")));
                applyFallbackLabels(relatedFacets);
                continue;
            }

            Set<IndexField> relatedFields = resourceTypeService.getResourceTypeIndexFields(relatedType);

            // A facet field may declare a different labelField than another facet field that
            // also points at the same relatedResourceType.  Group by labelField and issue one
            // getLabels() call per (relatedType, labelField) pair.
            Map<String, List<Facet>> byLabelField = new LinkedHashMap<>();
            for (Facet facet : relatedFacets) {
                IndexField indexField = fieldMap.get(facet.getField());
                String labelField = resolveLabelField(indexField, relatedFields);
                if (labelField == null) {
                    // No label field resolvable — apply proper-case fallback directly.
                    applyFallbackLabels(Collections.singletonList(facet));
                    continue;
                }
                byLabelField.computeIfAbsent(labelField, k -> new ArrayList<>()).add(facet);
            }

            for (Map.Entry<String, List<Facet>> lfEntry : byLabelField.entrySet()) {
                String labelField         = lfEntry.getKey();
                List<Facet> labelFacets   = lfEntry.getValue();
                List<String> ids = collectIds(labelFacets);

                if (ids.isEmpty()) {
                    continue;
                }

                Map<String, String> idToLabel;
                try {
                    idToLabel = searchService.getLabels(relatedType, primaryKeyField, ids, labelField);
                } catch (UnsupportedOperationException e) {
                    logger.warn("SearchService '{}' does not implement getLabels(); " +
                            "falling back to proper-case labels for resource type '{}'",
                            searchService.getClass().getSimpleName(), relatedType);
                    applyFallbackLabels(labelFacets);
                    return;
                } catch (Exception e) {
                    logger.warn("Failed to fetch labels for resource type '{}', labelField '{}': {}",
                            relatedType, labelField, e.getMessage());
                    applyFallbackLabels(labelFacets);
                    continue;
                }

                for (Facet facet : labelFacets) {
                    facet.getValues().forEach(v -> {
                        String label = idToLabel.get(v.getValue());
                        v.setLabel(label != null ? label : toProperCase(v.getValue()));
                    });
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a map of {@code IndexField.name → IndexField} for the given resource type.
     */
    private Map<String, IndexField> buildFieldMap(String resourceTypeName) {
        return resourceTypeService.getResourceTypeIndexFields(resourceTypeName)
                .stream()
                .collect(Collectors.toMap(IndexField::getName, f -> f));
    }

    /**
     * Returns the name of the primary-key {@link IndexField} for the given resource type,
     * or {@code null} if none is configured.
     */
    private String resolvePrimaryKeyField(String resourceType) {
        return resourceTypeService.getResourceTypeIndexFields(resourceType)
                .stream()
                .filter(IndexField::isPrimaryKey)
                .map(IndexField::getName)
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves which field name in the related resource type should be used as the display label.
     *
     * <ol>
     *   <li>Uses {@link IndexField#getRelatedResourceTypeField()} if explicitly configured.</li>
     *   <li>Falls back to a field named {@code "name"} in the related type.</li>
     *   <li>Returns {@code null} if neither is present — the caller will apply proper-case fallback.</li>
     * </ol>
     */
    private String resolveLabelField(IndexField sourceField, Set<IndexField> relatedFields) {
        if (sourceField.getRelatedResourceTypeField() != null) {
            return sourceField.getRelatedResourceTypeField();
        }
        return relatedFields.stream()
                .map(IndexField::getName)
                .filter("name"::equals)
                .findFirst()
                .orElse(null);
    }

    /**
     * Applies {@link #toProperCase(String)} as the label for every value in the given facets
     * that does not already have a label set.
     */
    private void applyFallbackLabels(List<Facet> facets) {
        for (Facet facet : facets) {
            facet.getValues().forEach(v -> {
                if (v.getLabel() == null) {
                    v.setLabel(toProperCase(v.getValue()));
                }
            });
        }
    }

    private List<String> collectIds(List<Facet> facets) {
        return facets.stream()
                .flatMap(f -> f.getValues().stream())
                .map(Value::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Converts a raw facet value into a human-readable string when no registry label is found.
     *
     * <p>Processing order:
     * <ol>
     *   <li>Split on {@code '-'}, capitalise each part, rejoin with {@code '-'}.</li>
     *   <li>Split the result on {@code '_'}, capitalise each part, rejoin with {@code ' '}.</li>
     * </ol>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "my_resource_type"} → {@code "My Resource Type"}</li>
     *   <li>{@code "some-thing_here"} → {@code "Some-Thing Here"}</li>
     *   <li>{@code ""} or {@code null} → {@code "-"}</li>
     * </ul>
     *
     * <p>Inspired by the {@code FacetEnrichmentAspect} from the observatory application.
     */
    static String toProperCase(String value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }
        return capitaliseWithDelimiter(
                capitaliseWithDelimiter(value, "-", "-"),
                "_", " ");
    }

    private static String capitaliseWithDelimiter(String str, String delimiter, String newDelimiter) {
        if (str.isEmpty()) {
            return "-";
        }
        StringJoiner joiner = new StringJoiner(newDelimiter);
        for (String part : str.split(delimiter, -1)) {
            joiner.add(part.isEmpty() ? "" : Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return joiner.toString();
    }
}
