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
import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.domain.VersionDTO;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.VersionService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = GenericController.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class GenericController {

    public static final String BASE_PATH = "records";

    private final GenericResourceService genericResourceService;
    private final VersionService versionService;

    public GenericController(GenericResourceService genericResourceService, VersionService versionService) {
        this.genericResourceService = genericResourceService;
        this.versionService = versionService;
    }

    @PostMapping(path = "{resourceType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> create(@PathVariable("resourceType") String resourceType,
                                         @RequestBody Object resource) {
        Object created = genericResourceService.add(resourceType, resource);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping(path = "{resourceType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> update(@PathVariable("resourceType") String resourceType,
                                         @RequestBody Object resource) {
        Object updated = genericResourceService.update(resourceType, resource);
        return ResponseEntity.ok(updated);
    }

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
     */
    @GetMapping("{resourceType}/key")
    public ResponseEntity<Object> getByKey(@PathVariable("resourceType") String resourceType,
                                           @RequestParam Map<String, String> keyValues) {
        return ResponseEntity.ok(genericResourceService.getByKey(resourceType, keyValues));
    }

    /**
     * Composite-primary-key equivalent of {@link #delete(String, String)}.
     *
     * @see #getByKey(String, Map) for the query-parameter convention this route shares
     */
    @DeleteMapping("{resourceType}/key")
    public ResponseEntity<Object> deleteByKey(@PathVariable("resourceType") String resourceType,
                                              @RequestParam Map<String, String> keyValues) {
        Object deleted = genericResourceService.deleteByKey(resourceType, keyValues);
        return ResponseEntity.ok(deleted);
    }

    @GetMapping(path = "{resourceType}")
    @BrowseParameters
    public ResponseEntity<Paging<Object>> browse(@Parameter(hidden = true)
                                                 @RequestParam MultiValueMap<String, Object> params,
                                                 @PathVariable("resourceType") String resourceType) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getResults(filter));
    }

    @GetMapping(path = "{resourceType}/semantic")
    @BrowseParameters
    public ResponseEntity<Paging<Object>> semanticBrowse(@Parameter(hidden = true)
                                                         @RequestParam MultiValueMap<String, Object> params,
                                                         @PathVariable("resourceType") String resourceType) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getSemanticResults(filter));
    }

    @GetMapping(path = "{resourceType}/hybrid")
    @BrowseParameters
    public ResponseEntity<Paging<Object>> hybridBrowse(@Parameter(hidden = true)
                                                       @RequestParam MultiValueMap<String, Object> params,
                                                       @PathVariable("resourceType") String resourceType) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.getHybridResults(filter));
    }

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

    @GetMapping("{resourceType}/{id}")
    public ResponseEntity<Object> get(@PathVariable("resourceType") String resourceType,
                                      @PathVariable("id") String id) {
        return ResponseEntity.ok(genericResourceService.get(resourceType, id));
    }

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
     * @param version the version label, i.e. {@link VersionDTO#getVersion()} from the
     *                {@code /versions} listing endpoint.
     */
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
     * @see #getByKey(String, Map) for the query-parameter convention this route shares
     */
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
     * @see #getByKey(String, Map) for the query-parameter convention this route shares
     */
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

    @GetMapping("{resourceType}/{id}/recommendations")
    @BrowseParameters
    public ResponseEntity<List<?>> recommend(@PathVariable("resourceType") String resourceType,
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
     * reserved by {@link FacetFilter#getFrom()} and still apply normally.
     */
    @GetMapping("{resourceType}/key/recommendations")
    @BrowseParameters
    public ResponseEntity<List<?>> recommendByKey(@PathVariable("resourceType") String resourceType,
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

    @PostMapping(path = "{resourceType}/recommendations", consumes = MediaType.APPLICATION_JSON_VALUE)
    @BrowseParameters
    public ResponseEntity<List<?>> recommendByResource(
            @PathVariable("resourceType") String resourceType,
            @RequestBody Object resource,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(genericResourceService.recommend(filter, resource));
    }
}
