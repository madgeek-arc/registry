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
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.domain.VersionDTO;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain-agnostic REST controller exposing CRUD, browse, and recommendation operations for
 * <em>any</em> registered {@code ResourceType}, addressed dynamically by the {@code resourceType}
 * path segment rather than fixed per-type at compile time.
 *
 * <p>This is the intended domain-facing alternative to the frozen {@code ResourceController}/
 * {@code SearchController}, and to per-type controllers built by extending
 * {@link AbstractGenericController} — use this one when the set of resource types isn't known
 * until runtime (e.g. a generic admin UI or a client that discovers types via
 * {@code ResourceTypeService}).
 *
 * <h2>Endpoints</h2>
 * <table border="1">
 *   <caption>Endpoint overview</caption>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>POST</td>   <td>/{resourceType}</td>                     <td>Create a new resource</td></tr>
 *   <tr><td>PUT</td>    <td>/{resourceType}</td>                     <td>Update an existing resource (identity from body)</td></tr>
 *   <tr><td>DELETE</td> <td>/{resourceType}/{id}</td>                 <td>Delete a resource by single primary key</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/key</td>                  <td>Get a resource by composite primary key</td></tr>
 *   <tr><td>DELETE</td> <td>/{resourceType}/key</td>                  <td>Delete a resource by composite primary key</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}</td>                      <td>Paginated browse with facets</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/semantic</td>              <td>Embedding-based semantic browse</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/hybrid</td>                <td>Combined lexical/semantic browse</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/highlighted</td>           <td>Browse with keyword-highlight fragments</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/hybrid/highlighted</td>    <td>Hybrid browse with highlights</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/{id}</td>                  <td>Fetch a single resource by single primary key</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/{id}/versions</td>         <td>List historical versions</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/{id}/versions/{version}</td> <td>Fetch one historical version</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/key/versions</td>          <td>List historical versions by composite key</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/key/versions/{version}</td> <td>Fetch one historical version by composite key</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/{id}/recommendations</td>  <td>Similar-resource recommendations by id</td></tr>
 *   <tr><td>GET</td>    <td>/{resourceType}/key/recommendations</td>   <td>Similar-resource recommendations by composite key</td></tr>
 *   <tr><td>POST</td>   <td>/{resourceType}/recommendations</td>       <td>Recommendations for an unsaved resource payload</td></tr>
 * </table>
 *
 * <h2>Single vs. composite primary keys</h2>
 * <p>{@code {id}}-based routes work only for resource types with exactly one
 * {@code primaryKey = true} field; a composite-key type used with them fails with a 400
 * ({@link gr.uoa.di.madgik.registry.exception.UnsupportedSearchParameterException}). The
 * {@code /key} routes are the literal-segment sibling that works for both — every query
 * parameter on those routes is treated as one primary-key field=value pair, and the resource
 * type's declared primary-key fields must all be present, no more, no fewer. {@code PUT} needs
 * neither variant: identity is always derived from the request body via
 * {@code extractPrimaryKeys}, regardless of key cardinality.
 *
 * <h2>Exception handling</h2>
 * <p>Unchecked exceptions (e.g.
 * {@link gr.uoa.di.madgik.registry.exception.ResourceNotFoundException}) propagate as-is and are
 * mapped to RFC 7807 {@link org.springframework.http.ProblemDetail} bodies by the library-default
 * {@code GlobalExceptionHandler}.
 *
 * <h2>Out of scope</h2>
 * <p>Authentication/authorization, CORS, rate limiting, and request logging are left to the
 * embedding application, consistent with every other controller in this repository — this
 * controller does not add any access control of its own.
 *
 * @see AbstractGenericController
 * @see GenericResourceService
 * @see FacetFilter
 */
@RestController
@RequestMapping(path = TypedResourceController.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class TypedResourceController {

    public static final String BASE_PATH = "records";

    private final GenericResourceService genericResourceService;
    private final VersionService versionService;

    public TypedResourceController(GenericResourceService genericResourceService, VersionService versionService) {
        this.genericResourceService = genericResourceService;
        this.versionService = versionService;
    }

    /**
     * Creates a new resource of the given type.
     *
     * @param resourceType the name of the {@code ResourceType} to create the resource under
     * @param resource     the domain object to persist
     * @param uriBuilder   injected by Spring MVC, pre-populated with the current request's
     *                     scheme/host/port/context-path; used to build the {@code Location} header
     * @return the persisted resource, potentially enriched with generated fields, wrapped in
     *         {@code 201 Created} with a {@code Location} header pointing at the new resource
     *         (the {@code {id}} route for a single primary key, the {@code /key} route for a
     *         composite one)
     */
    // Tag descriptions live here on purpose (not on the class - that would tag every method with both).
    // Move these two lines in another common method if you ever remove/retag create(), otherwise
    // Swagger UI will lose the group descriptions.
    @Tag(name = "Records (by id)", description = "Operations for resource types with a single primary key, addressed via the {id} path segment.")
    @Tag(name = "Records (by key)", description = "Operations for resource types with a composite primary key, addressed via /key?field=value... query parameters.")
    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Create a new resource",
            description = "Persists a new resource of the given resource type. Validation is applied before saving.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Resource created successfully"),
                    @ApiResponse(responseCode = "404", description = "Unknown resource type"),
                    @ApiResponse(responseCode = "409", description = "A resource with the same primary key already exists")
            }
    )
    @PostMapping(path = "{resourceType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> create(@PathVariable("resourceType") String resourceType,
                                         @RequestBody Object resource,
                                         UriComponentsBuilder uriBuilder) {
        Object created = genericResourceService.add(resourceType, resource);
        return ResponseEntity.created(buildLocation(uriBuilder, resourceType, created)).body(created);
    }

    /**
     * Builds the {@code Location} URI for a just-created resource, appending either an
     * {@code {id}} segment (single primary key) or a {@code /key?field=value...} query (composite
     * primary key) onto {@code /{BASE_PATH}/{resourceType}}. The injected {@code uriBuilder} only
     * carries the current request's scheme/host/port/context-path (not the mapped
     * {@code {resourceType}} path itself), so that prefix is added explicitly here. Path/query
     * values are percent-encoded via {@link UriComponentsBuilder#encode()} so a key value
     * containing a slash round-trips the same way {@code GET {resourceType}/{id}} expects it
     * (see the encoded-slash firewall support).
     */
    private URI buildLocation(UriComponentsBuilder uriBuilder, String resourceType, Object created) {
        Map<String, String> keyValues = genericResourceService.getPrimaryKeyValues(resourceType, created);
        UriComponentsBuilder builder = uriBuilder.path("/{base}/{resourceType}");
        if (keyValues.size() == 1) {
            String id = keyValues.values().iterator().next();
            return builder.pathSegment(id).buildAndExpand(BASE_PATH, resourceType).encode().toUri();
        }
        builder.pathSegment("key");
        keyValues.forEach(builder::queryParam);
        return builder.buildAndExpand(BASE_PATH, resourceType).encode().toUri();
    }

    /**
     * Updates an existing resource. Identity is derived entirely from the primary-key field(s)
     * in the request body — single or composite alike — so no {@code id} path segment is needed.
     *
     * @param resourceType the name of the {@code ResourceType} that owns the resource
     * @param resource     the updated domain object; must contain valid primary key field(s)
     * @return the updated resource wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Update a resource",
            description = "Replaces the payload of an existing resource. The primary key(s), single or composite, are read from the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            }
    )
    @PutMapping(path = "{resourceType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> update(@PathVariable("resourceType") String resourceType,
                                         @RequestBody Object resource) {
        Object updated = genericResourceService.update(resourceType, resource);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes the resource identified by its single primary key. Composite-key resource types
     * must use {@link #deleteByKey(String, Map)} instead.
     *
     * @param resourceType the name of the {@code ResourceType} that owns the resource
     * @param id           the identifier of the resource to delete
     * @return the deleted resource as it existed at deletion time, wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)"},
            summary = "Delete a resource by id",
            description = "Removes the resource with the given id and returns its last known state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "400", description = "Resource type has a composite primary key; use /key instead")
            }
    )
    @DeleteMapping("{resourceType}/{id}")
    public ResponseEntity<Object> delete(@PathVariable("resourceType") String resourceType,
                                         @PathVariable("id") String id) {
        Object deleted = genericResourceService.delete(resourceType, id);
        return ResponseEntity.ok(deleted);
    }

    /**
     * Composite-primary-key equivalent of {@link #get(String, String)}. Every query parameter
     * on this route is treated as a primary-key field=value pair; the resource type's declared
     * primary-key fields must all be present, and no others.
     *
     * @param resourceType the name of the {@code ResourceType} to search within
     * @param keyValues    one query parameter per declared primary-key field
     * @return the deserialized domain object wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by key)"},
            summary = "Get a resource by composite primary key",
            description = "Fetches a resource by its full primary key, single or composite. Every query parameter is treated as one primary-key field=value pair; the resource type's declared primary-key fields must all be present, and no others.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource found"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "400", description = "Query parameters don't exactly match the resource type's declared primary-key fields")
            }
    )
    @GetMapping("{resourceType}/key")
    public ResponseEntity<Object> getByKey(@PathVariable("resourceType") String resourceType,
                                           @RequestParam Map<String, String> keyValues) {
        return ResponseEntity.ok(genericResourceService.getByKey(resourceType, keyValues));
    }

    /**
     * Composite-primary-key equivalent of {@link #delete(String, String)}.
     *
     * @param resourceType the name of the {@code ResourceType} that owns the resource
     * @param keyValues    one query parameter per declared primary-key field
     * @return the deleted resource as it existed at deletion time, wrapped in {@code 200 OK}
     * @see #getByKey(String, Map) for the query-parameter convention this route shares
     */
    @Operation(
            tags = {"Records (by key)"},
            summary = "Delete a resource by composite primary key",
            description = "Removes the resource matching the given full primary key and returns its last known state.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "400", description = "Query parameters don't exactly match the resource type's declared primary-key fields")
            }
    )
    @DeleteMapping("{resourceType}/key")
    public ResponseEntity<Object> deleteByKey(@PathVariable("resourceType") String resourceType,
                                              @RequestParam Map<String, String> keyValues) {
        Object deleted = genericResourceService.deleteByKey(resourceType, keyValues);
        return ResponseEntity.ok(deleted);
    }

    /**
     * Returns a paginated, faceted listing of resources of the given type.
     *
     * @param params       the raw query parameters (keyword, from, quantity, sort/order, browseBy,
     *                      and arbitrary facet filters), bound via {@link FacetFilter#from(Map)}
     * @param resourceType the name of the {@code ResourceType} to browse
     * @return a {@link Paging} result containing the hits and computed facets, wrapped in
     *         {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Browse resources",
            description = "Returns a paginated, faceted list of resources. Supports filtering, sorting, and keyword search.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Results returned"),
                    @ApiResponse(responseCode = "400", description = "quantity exceeds the configured maximum")
            }
    )
    @GetMapping(path = "{resourceType}")
    @BrowseParameters
    public ResponseEntity<Paging<Object>> browse(@Parameter(hidden = true)
                                                 @RequestParam MultiValueMap<String, Object> params,
                                                 @PathVariable("resourceType") String resourceType) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getResults(filter));
    }

    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Browse resources semantically",
            description = "Returns a paginated list of resources using embedding-based semantic search."
    )
    @GetMapping(path = "{resourceType}/semantic")
    @BrowseParameters
    public ResponseEntity<Paging<Object>> semanticBrowse(@Parameter(hidden = true)
                                                         @RequestParam MultiValueMap<String, Object> params,
                                                         @PathVariable("resourceType") String resourceType) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getSemanticResults(filter));
    }

    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Browse resources with hybrid search",
            description = "Returns a paginated list of resources using combined lexical and semantic ranking."
    )
    @GetMapping(path = "{resourceType}/hybrid")
    @BrowseParameters
    public ResponseEntity<Paging<Object>> hybridBrowse(@Parameter(hidden = true)
                                                       @RequestParam MultiValueMap<String, Object> params,
                                                       @PathVariable("resourceType") String resourceType) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getHybridResults(filter));
    }

    /**
     * Returns a paginated listing of resources with keyword-highlight fragments for each hit.
     *
     * @param resourceType the name of the {@code ResourceType} to browse
     * @param params       the raw query parameters; a non-empty {@code keyword} is required for
     *                      highlights to be populated
     * @return a {@link Paging} of {@link HighlightedResult} wrappers, wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Browse resources with highlights",
            description = "Same as the standard browse endpoint but each result also carries keyword-highlight fragments."
    )
    @GetMapping("{resourceType}/highlighted")
    @BrowseParameters
    public ResponseEntity<Paging<HighlightedResult<Object>>> browseHighlighted(
            @PathVariable("resourceType") String resourceType,
            @Parameter(hidden = true)
            @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getHighlightedResults(filter));
    }

    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Browse resources with hybrid highlights",
            description = "Returns hybrid-ranked results with lexical highlights and semantic snippets."
    )
    @GetMapping("{resourceType}/hybrid/highlighted")
    @BrowseParameters
    public ResponseEntity<Paging<HighlightedResult<Object>>> browseHybridHighlighted(
            @PathVariable("resourceType") String resourceType,
            @Parameter(hidden = true)
            @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getHybridHighlightedResults(filter));
    }

    /**
     * Retrieves a single resource by its single primary key. Composite-key resource types must
     * use {@link #getByKey(String, Map)} instead.
     *
     * @param resourceType the name of the {@code ResourceType} to search within
     * @param id           the identifier of the resource to fetch
     * @return the deserialized domain object wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)"},
            summary = "Get a resource by id",
            description = "Fetches the resource with the given id from the given resource type.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resource found"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "400", description = "Resource type has a composite primary key; use /key instead")
            }
    )
    @GetMapping("{resourceType}/{id}")
    public ResponseEntity<Object> get(@PathVariable("resourceType") String resourceType,
                                      @PathVariable("id") String id) {
        return ResponseEntity.ok(genericResourceService.get(resourceType, id));
    }

    /**
     * Lists the historical versions of a resource identified by its single primary key.
     *
     * @param resourceType the name of the {@code ResourceType} that owns the resource
     * @param id           the identifier of the resource
     * @return the version history, newest first, wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)"},
            summary = "List a resource's versions",
            description = "Returns the historical versions of the resource with the given id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Versions returned"),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            }
    )
    @GetMapping("{resourceType}/{id}/versions")
    public ResponseEntity<List<VersionDTO<Object>>> getVersions(@PathVariable("resourceType") String resourceType,
                                                                 @PathVariable("id") String id) {
        Resource resource = genericResourceService.searchResource(resourceType, id, true);
        List<Version> versions = versionService.getVersionsByResource(resource.getId());
        List<VersionDTO<Object>> dtos = versions == null ? List.of() : versions.stream()
                .map(v -> toVersionDTO(resourceType, v))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Fetches a single historical version of a resource.
     *
     * @param resourceType the name of the {@code ResourceType} that owns the resource
     * @param id           the identifier of the resource
     * @param version the version label, i.e. {@link VersionDTO#getVersion()} from the
     *                {@code /versions} listing endpoint.
     * @return the version's payload as it existed at that point in time, wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)"},
            summary = "Get a single version",
            description = "Fetches one historical version of a resource by its version label.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Version found"),
                    @ApiResponse(responseCode = "404", description = "Resource or version not found")
            }
    )
    @GetMapping("{resourceType}/{id}/versions/{version}")
    public ResponseEntity<VersionDTO<Object>> getVersion(@PathVariable("resourceType") String resourceType,
                                                         @PathVariable("id") String id,
                                                         @PathVariable("version") String version) {
        Resource resource = genericResourceService.searchResource(resourceType, id, true);
        Version resourceVersion = versionService.getVersion(resource.getId(), version);
        if (resourceVersion == null) {
            throw new ResourceNotFoundException(version);
        }
        return ResponseEntity.ok(toVersionDTO(resourceType, resourceVersion));
    }

    /**
     * Composite-primary-key equivalent of {@link #getVersions(String, String)}.
     *
     * @param resourceType the name of the {@code ResourceType} that owns the resource
     * @param keyValues    one query parameter per declared primary-key field
     * @return the version history, newest first, wrapped in {@code 200 OK}
     * @see #getByKey(String, Map) for the query-parameter convention this route shares
     */
    @Operation(
            tags = {"Records (by key)"},
            summary = "List a resource's versions by composite primary key",
            description = "Returns the historical versions of the resource matching the given full primary key.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Versions returned"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "400", description = "Query parameters don't exactly match the resource type's declared primary-key fields")
            }
    )
    @GetMapping("{resourceType}/key/versions")
    public ResponseEntity<List<VersionDTO<Object>>> getVersionsByKey(@PathVariable("resourceType") String resourceType,
                                                                       @RequestParam Map<String, String> keyValues) {
        Resource resource = genericResourceService.searchResourceByKey(resourceType, keyValues, true);
        List<Version> versions = versionService.getVersionsByResource(resource.getId());
        List<VersionDTO<Object>> dtos = versions == null ? List.of() : versions.stream()
                .map(v -> toVersionDTO(resourceType, v))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Composite-primary-key equivalent of {@link #getVersion(String, String, String)}.
     *
     * @param resourceType the name of the {@code ResourceType} that owns the resource
     * @param keyValues    one query parameter per declared primary-key field
     * @param version      the version label, i.e. {@link VersionDTO#getVersion()} from the
     *                     {@code /key/versions} listing endpoint
     * @return the version's payload as it existed at that point in time, wrapped in {@code 200 OK}
     * @see #getByKey(String, Map) for the query-parameter convention this route shares
     */
    @Operation(
            tags = {"Records (by key)"},
            summary = "Get a single version by composite primary key",
            description = "Fetches one historical version of a resource, identified by its full primary key, by version label.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Version found"),
                    @ApiResponse(responseCode = "404", description = "Resource or version not found"),
                    @ApiResponse(responseCode = "400", description = "Query parameters don't exactly match the resource type's declared primary-key fields")
            }
    )
    @GetMapping("{resourceType}/key/versions/{version}")
    public ResponseEntity<VersionDTO<Object>> getVersionByKey(@PathVariable("resourceType") String resourceType,
                                                               @RequestParam Map<String, String> keyValues,
                                                               @PathVariable("version") String version) {
        Resource resource = genericResourceService.searchResourceByKey(resourceType, keyValues, true);
        Version resourceVersion = versionService.getVersion(resource.getId(), version);
        if (resourceVersion == null) {
            throw new ResourceNotFoundException(version);
        }
        return ResponseEntity.ok(toVersionDTO(resourceType, resourceVersion));
    }

    private VersionDTO<Object> toVersionDTO(String resourceType, Version version) {
        VersionDTO<Object> dto = new VersionDTO<>();
        dto.setVersion(version.getVersion());
        dto.setCreationDate(version.getCreationDate());
        dto.setResourceId(version.getResource() != null ? version.getResource().getId() : null);
        dto.setPayload(genericResourceService.get(resourceType, version));
        return dto;
    }

    /**
     * Returns resources similar to the one identified by its single primary key.
     *
     * @param resourceType the name of the {@code ResourceType} to search within
     * @param id           the identifier of the reference resource
     * @param params       the raw query parameters for pagination and additional filtering
     * @return an ordered list of similar resources wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)"},
            summary = "Get recommendations for a resource by id",
            description = "Returns resources similar to the one identified by id, ordered by descending similarity."
    )
    @GetMapping("{resourceType}/{id}/recommendations")
    @BrowseParameters
    public ResponseEntity<List<ScoredResult<Object>>> recommend(@PathVariable("resourceType") String resourceType,
                                             @PathVariable("id") String id,
                                             @Parameter(hidden = true)
                                             @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.recommend(filter, id));
    }

    /**
     * Composite-primary-key equivalent of {@link #recommend(String, String, MultiValueMap)}.
     *
     * <p>Unlike that endpoint, every non-reserved query parameter here is treated as part of the
     * composite primary key identifying the reference resource — {@link FacetFilter#getFilter()}
     * criteria orthogonal to the key (e.g. additional facet filters) are not supported on this
     * route, since the same query string cannot unambiguously carry both. {@code keyword},
     * {@code from}, {@code quantity}, {@code sort}/{@code order}, and {@code browseBy} are
     * reserved by {@link FacetFilter#from(Map)} and still apply normally.
     *
     * @param resourceType the name of the {@code ResourceType} to search within
     * @param params       the raw query parameters; every non-reserved entry is treated as one
     *                     primary-key field=value pair
     * @return an ordered list of similar resources wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by key)"},
            summary = "Get recommendations for a resource by composite primary key",
            description = "Returns resources similar to the one identified by its full primary key, ordered by descending similarity. Does not support additional facet-filter criteria in the same request (see method Javadoc).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Results returned"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "400", description = "Query parameters don't exactly match the resource type's declared primary-key fields")
            }
    )
    @GetMapping("{resourceType}/key/recommendations")
    @BrowseParameters
    public ResponseEntity<List<ScoredResult<Object>>> recommendByKey(@PathVariable("resourceType") String resourceType,
                                                  @Parameter(hidden = true)
                                                  @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        Map<String, String> keyValues = new LinkedHashMap<>();
        filter.getFilter().forEach((field, value) -> keyValues.put(field, firstValue(value)));
        filter.getFilter().clear();
        return ResponseEntity.ok(genericResourceService.recommendByKey(filter, keyValues));
    }

    private static String firstValue(Object value) {
        if (value instanceof List<?> list) {
            return list.isEmpty() ? null : String.valueOf(list.get(0));
        }
        return value == null ? null : value.toString();
    }

    /**
     * Returns resources similar to the given, not-necessarily-persisted resource payload.
     *
     * @param resourceType the name of the {@code ResourceType} to search within
     * @param resource     the domain object to use as the similarity anchor; does not need to be stored
     * @param params       the raw query parameters for pagination and additional filtering
     * @return an ordered list of similar resources wrapped in {@code 200 OK}
     */
    @Operation(
            tags = {"Records (by id)", "Records (by key)"},
            summary = "Get recommendations for a resource payload",
            description = "Returns resources similar to the provided resource payload, ordered by descending similarity. The resource does not need to be stored."
    )
    @PostMapping(path = "{resourceType}/recommendations", consumes = MediaType.APPLICATION_JSON_VALUE)
    @BrowseParameters
    public ResponseEntity<List<ScoredResult<Object>>> recommendByResource(
            @PathVariable("resourceType") String resourceType,
            @RequestBody Object resource,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.recommend(filter, resource));
    }
}
