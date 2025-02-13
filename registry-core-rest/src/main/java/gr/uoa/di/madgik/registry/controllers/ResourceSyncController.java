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

import gr.uoa.di.madgik.registry.service.ResourceSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class ResourceSyncController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceSyncController.class);

    @Autowired
    ResourceSyncService resourceSyncService;


    @RequestMapping(value = "/resourcesync/{name}/resourcelist.xml", method = RequestMethod.GET, headers = "Accept=application/xml")
    public ResponseEntity getResourceListController(@PathVariable("name") String name) {
        return new ResponseEntity(resourceSyncService.getResourceList(name).serialise(), HttpStatus.OK);
    }

    @RequestMapping(value = "/resourcesync/", method = RequestMethod.GET, headers = "Accept=application/xml")
    public ResponseEntity getCapabilityListController() {
        return new ResponseEntity(resourceSyncService.getCapabilityList().serialise(), HttpStatus.OK);
    }

    @RequestMapping(value = "/resourcesync/{resourceType}/{date}/changelist.xml", method = RequestMethod.GET, headers = "Accept=application/xml")
    public ResponseEntity getChangeListController(@PathVariable("resourceType") String resourceType, @PathVariable("date") Long date) {
        return new ResponseEntity(resourceSyncService.getChangeList(resourceType, new Date(date)).serialise(), HttpStatus.OK);
    }

}
