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

import gr.uoa.di.madgik.registry.service.ResourceSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
public class ResourceSyncController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceSyncController.class);

    private final ResourceSyncService resourceSyncService;

    public ResourceSyncController(ResourceSyncService resourceSyncService) {
        this.resourceSyncService = resourceSyncService;
    }

    @GetMapping(value = "/resourcesync/{name}/resourcelist.xml", headers = "Accept=application/xml")
    public ResponseEntity getResourceListController(@PathVariable("name") String name) {
        return new ResponseEntity(resourceSyncService.getResourceList(name).serialise(), HttpStatus.OK);
    }

    @GetMapping(value = "/resourcesync/", headers = "Accept=application/xml")
    public ResponseEntity getCapabilityListController() {
        return new ResponseEntity(resourceSyncService.getCapabilityList().serialise(), HttpStatus.OK);
    }

    @GetMapping(value = "/resourcesync/{resourceType}/{date}/changelist.xml", headers = "Accept=application/xml")
    public ResponseEntity getChangeListController(@PathVariable("resourceType") String resourceType, @PathVariable("date") Long date) {
        return new ResponseEntity(resourceSyncService.getChangeList(resourceType, new Date(date)).serialise(), HttpStatus.OK);
    }

}
