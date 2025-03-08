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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.IndexedFieldService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceTypeService resourceTypeService;
    private final IndexedFieldService indexedFieldService;

    public ResourceController(ResourceService resourceService, ResourceTypeService resourceTypeService,
                              IndexedFieldService indexedFieldService) {
        this.resourceService = resourceService;
        this.resourceTypeService = resourceTypeService;
        this.indexedFieldService = indexedFieldService;
    }

    @RequestMapping(value = "resource/resourceType", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<ResourceType> addResourceType(@RequestBody ResourceType resourceType) {
        resourceType.setCreationDate(new Date());
        resourceType.setModificationDate(new Date());
        try {
            resourceTypeService.addResourceType(resourceType);
            return new ResponseEntity<>(resourceType, HttpStatus.CREATED);
        } catch (ServiceException e) {
            throw new ServiceException(e);
        }
    }

    @RequestMapping(value = "/resources/indexed/{resourceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<IndexedField>> getIndexedFields(@PathVariable("resourceId") String resourceId) {
        return new ResponseEntity<>(indexedFieldService.getIndexedFields(resourceId), HttpStatus.OK);
    }


    @RequestMapping(value = "/resources/{resourceType}/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> getResourceById(@PathVariable("resourceType") String resourceType, @PathVariable("id") String id) {
        Resource resource = resourceService.getResource(id);
        if (resource == null) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(resource, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/resources/{resourceType}", params = {"from", "to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<Resource>> getResources(@PathVariable("resourceType") String resourceType,
                                                         @RequestParam(value = "from", defaultValue = "0") int from,
                                                         @RequestParam(value = "to", defaultValue = "10") int to) {
        List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType), from, to);
        int total = resourceService.getTotal(resourceTypeService.getResourceType(resourceType)).intValue();

        Paging<Resource> paging = new Paging<>(total, from, to, results, null);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @RequestMapping(value = "/resources/", params = {"from", "to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging<Resource>> getAllResources(@RequestParam(value = "from", defaultValue = "0") int from,
                                                            @RequestParam(value = "to", defaultValue = "10") int to) {
        List<Resource> results = resourceService.getResource(from, to);
        int total = resourceService.getTotal(null).intValue();

        Paging<Resource> paging = new Paging<>(total, from, to, results, null);
        return new ResponseEntity<>(paging, HttpStatus.OK);
    }

    @RequestMapping(value = "/resources", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> addResource(@RequestBody Resource resource) {
        resourceService.addResource(resource);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/resources/{resourceId}/{resourceType}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> changeResourceType(
            @PathVariable("resourceId") String resourceId,
            @PathVariable("resourceType") String resourceTypeName
    ) {
        Resource resource = resourceService.getResource(resourceId);
        if (resource == null)
            throw new ServiceException("Resource not found");

        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        if (resourceType == null)
            throw new ServiceException("Resource type not found");

        resourceService.changeResourceType(resource, resourceType);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/resources", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> updateResource(@RequestBody Resource resource) {
        resource.setModificationDate(new Date());
        Resource resourceFinal;
        resourceFinal = resourceService.updateResource(resource);
        return new ResponseEntity<>(resourceFinal, HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/resources/{id}", method = RequestMethod.DELETE)
    public void deleteResources(@PathVariable("id") String id) {
        resourceService.deleteResource(id);
    }

}
