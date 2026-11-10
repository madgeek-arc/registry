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

import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read access to resource versions under {@code /versions}. The {@code /version} paths are deprecated aliases,
 * to be removed in the next major release.
 */
@RestController
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping(value = "/versions/{resourceType}", headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersionsByResourceType(@PathVariable("resourceType") String resourceType) {
        return found(versionService.getVersionsByResourceType(resourceType));
    }

    @GetMapping(value = "/versions/{resourceType}/{resource}", headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersionsByResource(@PathVariable("resourceType") String resourceType,
                                                               @PathVariable("resource") String resource) {
        return found(versionService.getVersionsByResource(resource));
    }

    @GetMapping(value = "/versions/{resourceType}/{resource}/{version}", headers = "Accept=application/json")
    public ResponseEntity<Version> getVersion(@PathVariable("resourceType") String resourceType,
                                              @PathVariable("resource") String resource,
                                              @PathVariable("version") String versionNumber) {
        Version version = versionService.getVersion(resource, versionNumber);
        if (version == null) {
            throw new ResourceNotFoundException("Version not found");
        }
        return new ResponseEntity<>(version, HttpStatus.OK);
    }

    @GetMapping(value = "/versions", headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersions() {
        return found(versionService.getAllVersions());
    }

    @Deprecated(forRemoval = true)
    @Operation(deprecated = true, summary = "Deprecated: use /versions/{resourceType}")
    @GetMapping(value = "/version/{resourceType}", headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersionsByResourceTypeDeprecated(
            @PathVariable("resourceType") String resourceType) {
        return getVersionsByResourceType(resourceType);
    }

    @Deprecated(forRemoval = true)
    @Operation(deprecated = true, summary = "Deprecated: use /versions/{resourceType}/{resource}")
    @GetMapping(value = "/version/{resourceType}/{resource}", headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersionsByResourceDeprecated(
            @PathVariable("resourceType") String resourceType, @PathVariable("resource") String resource) {
        return getVersionsByResource(resourceType, resource);
    }

    @Deprecated(forRemoval = true)
    @Operation(deprecated = true, summary = "Deprecated: use /versions/{resourceType}/{resource}/{version}")
    @GetMapping(value = "/version/{resourceType}/{resource}/{version}", headers = "Accept=application/json")
    public ResponseEntity<Version> getVersionDeprecated(@PathVariable("resourceType") String resourceType,
                                                        @PathVariable("resource") String resource,
                                                        @PathVariable("version") String versionNumber) {
        return getVersion(resourceType, resource, versionNumber);
    }

    @Deprecated(forRemoval = true)
    @Operation(deprecated = true, summary = "Deprecated: use /versions")
    @GetMapping(value = "/version", headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersionsDeprecated() {
        return getVersions();
    }

    private static ResponseEntity<List<Version>> found(List<Version> versions) {
        if (versions == null || versions.isEmpty()) {
            throw new ResourceNotFoundException("Version not found");
        }
        return new ResponseEntity<>(versions, HttpStatus.OK);
    }
}
