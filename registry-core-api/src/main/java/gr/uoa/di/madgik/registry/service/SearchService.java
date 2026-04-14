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

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.*;
import java.util.stream.Collectors;


public interface SearchService {

    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType, int quantity, int from, String sortByField, String sortOrder);

    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType);

    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> search(FacetFilter filter) throws ServiceException;

    /**
     * Recommends resources that are similar to the resource identified by the given {@code resourceIdAndValue},
     * further constrained by the provided {@code filter}.
     *
     * @param filter             the additional filter criteria to apply
     * @param resourceIdAndValue the (field, value) pair used to resolve the reference resource for similarity matching
     * @return a list of recommended resources
     * @throws ServiceException if the reference resource cannot be retrieved or the recommendation query fails
     */
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    List<Resource> recommend(FacetFilter filter, KeyValue resourceIdAndValue) throws ServiceException;

    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException;

    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) throws ServiceException;

    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException;

    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category);

    /**
     * Returns a map of {@code idField} value → {@code labelField} value by fetching resources of
     * the given {@code resourceType} whose {@code idField} matches any entry in {@code ids}.
     * Implementations are expected to execute a single batch query regardless of how many IDs
     * are requested.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}. Backends that
     * support label enrichment (SQL, Elasticsearch) must override this method.
     *
     * @param resourceType the name of the ResourceType index / view to query
     * @param idField      the IndexField name used as the identifier (typically {@code primaryKey=true})
     * @param ids          the list of ID values to look up; must not be {@code null}
     * @param labelField   the IndexField name whose value should be used as the display label
     * @return an immutable-safe map from id value to label value;
     *         entries are absent when no matching resource was found
     */
    default Map<String, String> getLabels(String resourceType, String idField,
                                          List<String> ids, String labelField) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not implement getLabels()");
    }

    /**
     * Derives the effective {@code browseBy} field list for a search request.
     *
     * <p>First computes the intersection of labeled {@link IndexField} names across all supplied
     * resource types (alias groups only expose fields present in every member). When
     * {@code requestedBrowseBy} is provided, only the requested fields that still exist in that
     * intersection are kept. If none of the requested fields are valid, the full available
     * intersection is returned instead.</p>
     *
     * @param resourceTypes     the resolved resource types for the query (direct or alias group)
     * @param requestedBrowseBy caller-supplied fields to preserve when they still exist
     * @return ordered, deduplicated list of effective browse-by fields
     */
    static List<String> resolveBrowseBy(List<ResourceType> resourceTypes, List<String> requestedBrowseBy) {
        Set<String> availableBrowseBy = null;
        for (ResourceType rt : resourceTypes) {
            Set<String> labeledFields = rt.getIndexFields().stream()
                    .filter(f -> f.getLabel() != null)
                    .map(IndexField::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (availableBrowseBy == null) {
                availableBrowseBy = labeledFields;
            } else {
                availableBrowseBy.retainAll(labeledFields);
            }
        }

        if (availableBrowseBy == null) {
            return new ArrayList<>();
        }

        if (requestedBrowseBy == null || requestedBrowseBy.isEmpty()) {
            return new ArrayList<>(availableBrowseBy);
        }

        LinkedHashSet<String> browseBy = requestedBrowseBy.stream()
                .filter(availableBrowseBy::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return browseBy.isEmpty()
                ? new ArrayList<>(availableBrowseBy)
                : new ArrayList<>(browseBy);
    }

    class KeyValue {

        public String field;

        public String value;

        public KeyValue(String field, String value) {
            this.field = field;
            this.value = value;
        }

        public KeyValue() {
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
