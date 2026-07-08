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

import java.util.List;

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

    @PutMapping(path = "{resourceType}/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> update(@PathVariable("resourceType") String resourceType,
                                         @PathVariable("id") String id,
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
