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

package gr.uoa.di.madgik.registry.controllers;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Abstract base REST controller that exposes a standard CRUD and search API for a single
 * registry resource type.
 *
 * <h2>Usage</h2>
 * <p>Subclasses must:
 * <ol>
 *   <li>Annotate the concrete class with {@code @RequestMapping} to provide the URL base path.</li>
 *   <li>Implement {@link #getResourceTypeName()} to return the registry resource-type name that
 *       this controller manages.</li>
 * </ol>
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping(path = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
 * public class ProviderController extends AbstractGenericController<Provider> {
 *
 *     public ProviderController(GenericResourceService genericResourceService) {
 *         super(genericResourceService);
 *     }
 *
 *     @Override
 *     protected String getResourceTypeName() {
 *         return "provider";
 *     }
 * }
 * }</pre>
 *
 * <h2>Endpoints</h2>
 * <table border="1">
 *   <caption>Endpoint overview</caption>
 *   <tr><th>Method</th><th>Path (relative to subclass mapping)</th><th>Description</th></tr>
 *   <tr><td>POST</td>   <td>/</td>          <td>Create a new resource</td></tr>
 *   <tr><td>PUT</td>    <td>/{id}</td>       <td>Update an existing resource</td></tr>
 *   <tr><td>DELETE</td> <td>/{id}</td>       <td>Delete a resource</td></tr>
 *   <tr><td>GET</td>    <td>/</td>           <td>Paginated browse with facets</td></tr>
 *   <tr><td>GET</td>    <td>/highlighted</td><td>Browse with keyword-highlight fragments</td></tr>
 *   <tr><td>GET</td>    <td>/{id}</td>       <td>Fetch a single resource by id</td></tr>
 *   <tr><td>GET</td>    <td>/{id}/recommendations</td><td>Similar-resource recommendations</td></tr>
 * </table>
 *
 * <h2>Type parameter</h2>
 * <p>{@code T} is the domain type managed by this controller.  It must match the class registered
 * as the {@code class} property of the corresponding {@code ResourceType}.  The controller itself
 * does not enforce the type at compile time — see {@link GenericResourceService} for the
 * runtime contract.
 *
 * <h2>Exception handling</h2>
 * <p>Unchecked exceptions (e.g.
 * {@link gr.uoa.di.madgik.registry.exception.ResourceNotFoundException}) propagate as-is
 * and should be handled by a global {@code @ControllerAdvice}.
 *
 * @param <T> the domain type managed by this controller
 *
 * @see GenericResourceService
 * @see FacetFilter
 */
@RestController
@Tag(name = "Generic Resource Controller")
public abstract class AbstractGenericController<T> {

    /** The service used to perform all read and write operations on the registry. */
    protected final GenericResourceService genericResourceService;

    /**
     * Constructs the controller with the required service dependency.
     *
     * @param genericResourceService the registry service; must not be {@code null}
     */
    protected AbstractGenericController(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    /**
     * Returns the registry resource-type name that this controller manages.
     *
     * <p>The value must match the {@code name} attribute of a registered {@code ResourceType}.
     * It is used as the implicit {@code resourceType} argument for every service call.
     *
     * @return the resource-type name, never {@code null} or empty
     */
    protected abstract String getResourceTypeName();

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new resource of the managed type.
     *
     * <p>The request body is deserialized into {@code T} and persisted via
     * {@link GenericResourceService#add(String, Object)}.  Validation is enabled by default.
     *
     * @param resource the domain object to persist
     * @return the persisted resource, potentially enriched with a generated id, wrapped in
     *         {@code 201 Created}
     */
    @Operation(
            summary = "Create a new resource",
            description = "Persists a new resource of the managed type. Validation is applied before saving.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Resource created successfully"),
                    @ApiResponse(responseCode = "409", description = "A resource with the same primary key already exists")
            }
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> create(@RequestBody T resource) {
        T created = genericResourceService.add(getResourceTypeName(), resource);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing resource.
     *
     * <p>The resource to update is located by extracting the primary key field(s) from the
     * request body. Validation is applied before the update is persisted.
     *
     * @param id       the identifier used in the URL path (for REST routing purposes)
     * @param resource the updated domain object; must contain valid primary key field(s)
     * @return the updated resource wrapped in {@code 200 OK}
     */
    @Operation(
            summary = "Update a resource",
            description = "Replaces the payload of an existing resource. The primary key(s) are read from the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            }
    )
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> update(@PathVariable("id") String id, @RequestBody T resource) {
        T updated = genericResourceService.update(getResourceTypeName(), resource);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes the resource identified by {@code id}.
     *
     * @param id the identifier of the resource to delete
     * @return the deleted resource as it existed at deletion time, wrapped in {@code 200 OK}
     */
    @Operation(
            summary = "Delete a resource",
            description = "Removes the resource with the given id and returns its last known state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<T> delete(@PathVariable("id") String id) {
        T deleted = genericResourceService.delete(getResourceTypeName(), id);
        return ResponseEntity.ok(deleted);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated, faceted listing of resources of the managed type.
     *
     * <p>All standard browse parameters (keyword, from, quantity, order, orderField, filter)
     * are bound from query parameters via {@link FacetFilter#from(Map)}.
     * The {@code resourceType} query parameter is automatically filled from
     * {@link #getResourceTypeName()}.
     *
     * @param params the raw query parameters from the HTTP request
     * @return a {@link Paging} result containing the hits and computed facets,
     *         wrapped in {@code 200 OK}
     */
    @Operation(
            summary = "Browse resources",
            description = "Returns a paginated, faceted list of resources. Supports filtering, sorting, and keyword search."
    )
    @BrowseParameters
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<T>> browse(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(getResourceTypeName());
        Paging<T> results = genericResourceService.getResults(filter);
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Browse resources semantically",
            description = "Returns a paginated list of resources using embedding-based semantic search."
    )
    @BrowseParameters
    @GetMapping(path = "/semantic", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<T>> browseSemantic(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(getResourceTypeName());
        Paging<T> results = genericResourceService.getSemanticResults(filter);
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Browse resources with hybrid search",
            description = "Returns a paginated list of resources using combined lexical and semantic ranking."
    )
    @BrowseParameters
    @GetMapping(path = "/hybrid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<T>> browseHybrid(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(getResourceTypeName());
        Paging<T> results = genericResourceService.getHybridResults(filter);
        return ResponseEntity.ok(results);
    }

    /**
     * Returns a paginated listing of resources with keyword-highlight fragments for each hit.
     *
     * <p>Highlight fragments mark the portions of the payload that matched the search keyword,
     * enabling UIs to render bold or underlined match snippets.  A non-empty {@code keyword}
     * parameter is required for highlights to be populated.
     *
     * @param params the raw query parameters from the HTTP request
     * @return a {@link Paging} of {@link HighlightedResult} wrappers, each containing the
     *         deserialized domain object and a map of field-name to highlight fragments,
     *         wrapped in {@code 200 OK}
     */
    @Operation(
            summary = "Browse resources with highlights",
            description = "Same as the standard browse endpoint but each result also carries keyword-highlight fragments."
    )
    @BrowseParameters
    @GetMapping(path = "/highlighted", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<HighlightedResult<T>>> browseHighlighted(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(getResourceTypeName());
        Paging<HighlightedResult<T>> results = genericResourceService.getHighlightedResults(filter);
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Browse resources with hybrid highlights",
            description = "Returns hybrid-ranked results with lexical highlights and semantic snippets."
    )
    @BrowseParameters
    @GetMapping(path = "/hybrid/highlighted", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<HighlightedResult<T>>> browseHybridHighlighted(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(getResourceTypeName());
        Paging<HighlightedResult<T>> results = genericResourceService.getHybridHighlightedResults(filter);
        return ResponseEntity.ok(results);
    }

    /**
     * Retrieves a single resource by its primary identifier.
     *
     * @param id the identifier of the resource to fetch
     * @return the deserialized domain object wrapped in {@code 200 OK}
     */
    @Operation(
            summary = "Get a resource by id",
            description = "Fetches the resource with the given id from the managed resource type.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource found"),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            }
    )
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> get(@PathVariable("id") String id) {
        T result = genericResourceService.get(getResourceTypeName(), id);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns resources that are similar to the resource identified by {@code id}.
     *
     * <p>Standard browse parameters (from, quantity, keyword, etc.) narrow the result set.
     * Results are returned in descending similarity order.
     *
     * @param id     the identifier of the reference resource
     * @param params the raw query parameters for pagination and additional filtering
     * @return an ordered list of similar resources wrapped in {@code 200 OK}
     */
    @Operation(
            summary = "Get recommendations for a resource",
            description = "Returns resources similar to the one identified by {id}, ordered by descending similarity."
    )
    @BrowseParameters
    @GetMapping(path = "/{id}/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ScoredResult<T>>> recommend(
            @PathVariable("id") String id,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(getResourceTypeName());
        List<ScoredResult<T>> results = genericResourceService.recommend(filter, id);
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Get recommendations for a resource payload",
            description = "Returns resources similar to the provided resource payload, ordered by descending similarity. The resource does not need to be stored."
    )
    @BrowseParameters
    @PostMapping(path = "/recommendations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ScoredResult<T>>> recommendByResource(
            @RequestBody T resource,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(getResourceTypeName());
        List<ScoredResult<T>> results = genericResourceService.recommend(filter, resource);
        return ResponseEntity.ok(results);
    }
}
