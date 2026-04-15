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

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Resource;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * High-level, type-safe abstraction over the registry persistence layer.
 *
 * <p>Where {@link ResourceService} operates on raw {@link Resource} wrappers,
 * {@code GenericResourceService} transparently serializes and deserializes the
 * {@code payload} field so callers work directly with their own domain objects.
 * The mapping between a Java type and a registry {@code ResourceType} is resolved
 * at runtime via the {@code resourceTypeName} parameter that every mutating method
 * accepts.
 *
 * <h2>Type safety</h2>
 * <p>All read and write methods use unchecked generic return types ({@code <T>}).
 * The concrete class resolved for a given {@code resourceTypeName} is determined
 * by the implementation; callers must ensure they pass the correct type at the
 * call site. The pattern is intentionally flexible so that a single service bean
 * can serve every resource type in the registry without requiring one service
 * implementation per type.
 *
 * <h2>Validation</h2>
 * <p>Mutating operations ({@link #add} and {@link #update}) provide overloads
 * that accept a {@code validate} flag. When {@code true}, implementations should
 * apply any registered validation logic (e.g. JSON-schema validation, vocabulary
 * checks) before persisting.  When {@code false}, validation is skipped — useful
 * for bulk imports or trusted internal operations.
 *
 * <h2>Intended usage</h2>
 * <pre>{@code
 * // Inject the service
 * @Autowired
 * private GenericResourceService genericResourceService;
 *
 * // Fetch a typed resource
 * Provider provider = genericResourceService.get("provider", "provider_id", "prov-001", false);
 *
 * // Persist a typed resource
 * Provider saved = genericResourceService.add("provider", provider);
 *
 * // Browse with filtering and facets
 * FacetFilter filter = new FacetFilter();
 * filter.setResourceType("provider");
 * filter.setQuantity(10);
 * Paging<Provider> results = genericResourceService.getResults(filter);
 * }</pre>
 *
 * @see ResourceService
 * @see SearchService
 */
public interface GenericResourceService {

    /**
     * Retrieves a single resource of the given type by matching one or more indexed field values.
     *
     * <p>All supplied {@link SearchService.KeyValue} pairs are combined with AND logic.
     *
     * @param resourceTypeName the name of the {@code ResourceType} to search within
     * @param keyValues        one or more field–value pairs that the resource must satisfy
     * @param <T>              the expected domain type
     * @return the deserialized domain object, or {@code null} if no match is found
     */
    <T> T get(String resourceTypeName, SearchService.KeyValue... keyValues);

    /**
     * Retrieves a single resource of the given type by its primary identifier.
     *
     * @param resourceTypeName the name of the {@code ResourceType} to search within
     * @param id               the resource identifier
     * @param <T>              the expected domain type
     * @return the deserialized domain object
     * @throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException if no resource with
     *         the given id exists
     */
    <T> T get(String resourceTypeName, String id);

    /**
     * Returns a paginated, faceted list of resources matching the given filter.
     *
     * @param filter the search and pagination criteria; {@link FacetFilter#getResourceType()}
     *               must be set to a valid resource type name
     * @param <T>    the expected domain type of each result
     * @return a {@link Paging} containing the results and computed facets
     */
    <T> Paging<T> getResults(FacetFilter filter);

    <T> Paging<T> getSemanticResults(FacetFilter filter);

    <T> Paging<T> getHybridResults(FacetFilter filter);

    /**
     * Returns a paginated, faceted list of resources matching the given filter, with the
     * facet list post-processed by the supplied transformer.
     *
     * <p>Use this overload when you need to reorder, remove, or rename facets after the
     * search engine has computed them — e.g. to suppress internal facets from the response.
     *
     * @param filter      the search and pagination criteria
     * @param transformer a {@link UnaryOperator} applied to the raw facet list before the
     *                    {@link Paging} is constructed; must not be {@code null}
     * @param <T>         the expected domain type of each result
     * @return a {@link Paging} containing the results and the transformed facets
     */
    <T> Paging<T> getResults(FacetFilter filter, UnaryOperator<List<Facet>> transformer);

    /**
     * Returns a paginated, faceted list of resources with highlight fragments for each hit.
     *
     * <p>Highlight fragments mark the portions of the payload that matched the search
     * keyword, enabling UIs to render bold/underlined matches.
     *
     * @param filter the search and pagination criteria; a non-empty keyword is required for
     *               highlights to be populated
     * @param <T>    the expected domain type of each result
     * @return a {@link Paging} of {@link HighlightedResult} wrappers
     */
    <T> Paging<HighlightedResult<T>> getHighlightedResults(FacetFilter filter);

    /**
     * Returns resources grouped by the values of a categorical field.
     *
     * <p>The returned map's keys are the distinct values of the {@code category} field and
     * the values are the lists of matching resources. Useful for building category-browse UIs.
     *
     * @param filter   the search and pagination criteria
     * @param category the indexed field name whose distinct values become the map keys
     * @param <T>      the expected domain type of each result
     * @return a map from category value to list of matching resources
     */
    <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category);

    /**
     * Returns resources that are similar to the resource identified by {@code id},
     * further constrained by the provided {@code filter}.
     *
     * <p>Similarity is typically computed using vector embeddings when the backing
     * search service supports them, falling back to keyword overlap otherwise.
     *
     * @param filter the additional filter and pagination criteria to apply
     * @param id     the identifier of the reference resource used as the similarity anchor
     * @param <T>    the expected domain type of each result
     * @return an ordered list of similar resources (most similar first)
     */
    <T> List<T> recommend(FacetFilter filter, String id);

    /**
     * Persists a new resource with validation enabled.
     *
     * <p>Equivalent to {@code add(resourceTypeName, resource, true)}.
     *
     * @param resourceTypeName the name of the {@code ResourceType} under which to store the resource
     * @param resource         the domain object to persist
     * @param <T>              the domain type
     * @return the persisted domain object, potentially enriched with generated fields (e.g. id)
     * @throws gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException if a resource
     *         with the same primary key already exists
     */
    <T> T add(String resourceTypeName, T resource);

    /**
     * Persists a new resource, optionally skipping validation.
     *
     * @param resourceTypeName the name of the {@code ResourceType} under which to store the resource
     * @param resource         the domain object to persist
     * @param validate         if {@code true}, validation is applied before persisting;
     *                         pass {@code false} for trusted bulk imports
     * @param <T>              the domain type
     * @return the persisted domain object, potentially enriched with generated fields (e.g. id)
     * @throws gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException if a resource
     *         with the same primary key already exists
     */
    <T> T add(String resourceTypeName, T resource, boolean validate);

    /**
     * Updates an existing resource with validation enabled.
     *
     * <p>The resource to update is located by extracting the values of all {@code primaryKey}
     * {@link gr.uoa.di.madgik.registry.domain.index.IndexField}s from the serialized payload.
     * Equivalent to {@code update(resourceTypeName, resource, true)}.
     *
     * @param resourceTypeName the name of the {@code ResourceType} that owns the resource
     * @param resource         the updated domain object; must contain valid primary key field(s)
     * @param <T>              the domain type
     * @return the updated domain object as it was persisted
     * @throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException if no resource
     *         matching the primary key(s) exists
     * @throws ServiceException if the resource type has no primary key fields defined
     */
    <T> T update(String resourceTypeName, T resource);

    /**
     * Updates an existing resource, optionally skipping validation.
     *
     * @param resourceTypeName the name of the {@code ResourceType} that owns the resource
     * @param resource         the updated domain object; must contain valid primary key field(s)
     * @param validate         if {@code true}, validation is applied before persisting;
     *                         pass {@code false} for trusted bulk operations
     * @param <T>              the domain type
     * @return the updated domain object as it was persisted
     * @throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException if no resource
     *         matching the primary key(s) exists
     * @throws ServiceException if the resource type has no primary key fields defined
     */
    <T> T update(String resourceTypeName, T resource, boolean validate);

    /**
     * Returns {@code true} if a resource matching the primary key field(s) of {@code resource}
     * already exists in the registry.
     *
     * <p>The primary key(s) are extracted from the serialized payload using the
     * {@link gr.uoa.di.madgik.registry.domain.index.IndexField}s marked with
     * {@code primaryKey = true} on the {@code ResourceType}.
     *
     * @param resourceTypeName the name of the {@code ResourceType} to search within
     * @param resource         the domain object whose primary key(s) are used for the lookup
     * @param <T>              the domain type
     * @return {@code true} if a matching resource exists, {@code false} otherwise
     * @throws ServiceException if the resource type has no primary key fields defined
     */
    <T> boolean exists(String resourceTypeName, T resource);

    /**
     * Deletes the resource with the given id from the given resource type.
     *
     * @param resourceTypeName the name of the {@code ResourceType} that owns the resource
     * @param id               the identifier of the resource to delete
     * @param <T>              the domain type
     * @return the domain object as it was at the time of deletion
     * @throws gr.uoa.di.madgik.registry.exception.ResourceNotFoundException if no resource with
     *         the given id exists
     */
    <T> T delete(String resourceTypeName, String id);

    /**
     * Validates the given resource against the registered validation rules for its type,
     * without persisting it.
     *
     * <p>Delegates to the {@link ResourceValidator} bean registered in the application context.
     * If no {@code ResourceValidator} bean is present, the resource is returned unchanged.
     *
     * @param resourceTypeName the name of the {@code ResourceType} whose validation rules apply
     * @param resource         the domain object to validate
     * @param <T>              the domain type
     * @return the (potentially normalised) domain object if validation passes
     * @throws RuntimeException if validation fails; the concrete exception type is determined
     *         by the {@link ResourceValidator} implementation in use
     * @see ResourceValidator
     */
    <T> T validate(String resourceTypeName, T resource);

    /**
     * Returns the Java class that the given resource type's payloads are deserialized into.
     *
     * @param resourceTypeName the name of the {@code ResourceType}
     * @return the corresponding Java class, never {@code null}
     * @throws ServiceException if the resource type is not registered or has no mapped class
     */
    Class<?> getClassFromResourceType(String resourceTypeName);

    /**
     * Searches for the raw {@link Resource} wrapper of a resource by its primary identifier.
     *
     * <p>Use this when you need access to registry metadata (version, dates, payload format)
     * rather than the deserialized domain object.
     *
     * @param resourceTypeName the name of the {@code ResourceType} to search within
     * @param id               the resource identifier
     * @param throwOnNull      if {@code true}, throws an exception when no resource is found;
     *                         if {@code false}, returns {@code null} instead
     * @return the raw {@link Resource}, or {@code null} if not found and {@code throwOnNull}
     *         is {@code false}
     */
    Resource searchResource(String resourceTypeName, String id, boolean throwOnNull);

    /**
     * Searches for the raw {@link Resource} wrapper by matching one or more indexed field values.
     *
     * @param resourceTypeName the name of the {@code ResourceType} to search within
     * @param keyValues        one or more field–value pairs that the resource must satisfy
     * @return the raw {@link Resource}, or {@code null} if no match is found
     */
    Resource searchResource(String resourceTypeName, SearchService.KeyValue... keyValues);
}
