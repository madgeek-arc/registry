/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @RequestMapping(value = "/version/{resourceType}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersionsByResourceType(@PathVariable("resourceType") String resourceType) {

        List<Version> versions = versionService.getVersionsByResourceType(resourceType);

        if (versions == null || versions.isEmpty()) {
            throw new ResourceNotFoundException("Version not found");
        } else {
            return new ResponseEntity<>(versions, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/version/{resourceType}/{resource}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersionsByResource(@PathVariable("resourceType") String resourceType,
                                                               @PathVariable("resource") String resource) {
        List<Version> versions = versionService.getVersionsByResource(resource);
        versions.stream().filter(v -> v.getVersion() == "1").collect(Collectors.toList());

        if (versions == null || versions.isEmpty()) {
            throw new ResourceNotFoundException("Version not found");
        } else {
            return new ResponseEntity<>(versions, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/version/{resourceType}/{resource}/{version}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<Version> getVersion(@PathVariable("resourceType") String resourceType,
                                              @PathVariable("resource") String resource,
                                              @PathVariable("version") String versionNumber) {

        Version version = versionService.getVersion(resource, versionNumber);

        if (version == null) {
            throw new ResourceNotFoundException("Version not found");
        } else {
            return new ResponseEntity<>(version, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/version", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<List<Version>> getVersions() {

        List<Version> versions = versionService.getAllVersions();

        if (versions == null || versions.isEmpty()) {
            throw new ResourceNotFoundException("Version not found");
        } else {
            return new ResponseEntity<>(versions, HttpStatus.OK);
        }

    }

}
