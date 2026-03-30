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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.*;
import java.util.stream.Collectors;


public interface SearchService {

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType, int quantity, int from, String sortByField, String sortOrder);

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType);

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> search(FacetFilter filter) throws ServiceException;

    /**
     * Recommends resources that are similar to the resource identified by the given {@code resourceIdAndValue},
     * further constrained by the provided {@code filter}.
     *
     * @param filter the additional filter criteria to apply
     * @param resourceIdAndValue the (field, value) pair used to resolve the reference resource for similarity matching
     * @return a list of recommended resources
     * @throws ServiceException if the reference resource cannot be retrieved or the recommendation query fails
     */
    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    List<Resource> recommend(FacetFilter filter, KeyValue resourceIdAndValue) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<HighlightedResult<Resource>> searchWithHighlights(FacetFilter filter) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
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
     * <p>Starts with any fields in {@code existingBrowseBy}, then appends the intersection of
     * labeled {@link IndexField} names across all supplied resource types (alias groups use
     * intersection so only fields present in every member are shown).</p>
     *
     * @param resourceTypes    the resolved resource types for the query (direct or alias group)
     * @param existingBrowseBy caller-supplied fields to include regardless (may be {@code null})
     * @return ordered, deduplicated list of field names to browse by
     */
    static List<String> resolveBrowseBy(List<ResourceType> resourceTypes, List<String> existingBrowseBy) {
        Set<String> browseBy = new LinkedHashSet<>();
        if (existingBrowseBy != null) {
            browseBy.addAll(existingBrowseBy);
        }
        Set<String> fromConfig = null;
        for (ResourceType rt : resourceTypes) {
            Set<String> labeled = rt.getIndexFields().stream()
                    .filter(f -> f.getLabel() != null)
                    .map(IndexField::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (fromConfig == null) {
                fromConfig = labeled;
            } else {
                fromConfig.retainAll(labeled);
            }
        }
        if (fromConfig != null) {
            browseBy.addAll(fromConfig);
        }
        return new ArrayList<>(browseBy);
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