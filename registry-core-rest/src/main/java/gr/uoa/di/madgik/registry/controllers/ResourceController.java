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
    public ResponseEntity getIndexedFields(@PathVariable("resourceId") String resourceId) {
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

    @RequestMapping(value = "/resources/{resourceType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType) {
        List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType));
        Paging paging = new Paging(results.size(), 0, results.size() - 1, results, null);
        if (results.size() == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/resources/{resourceType}", params = {"from"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType, @RequestParam(value = "from") int from) {
        // FIXME: very inefficient..
        //  create method returning the size of the results instead
        List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType), from, 0);
        int total = resourceService.getResource(resourceTypeService.getResourceType(resourceType)).size();
        // -------------------------

        Paging paging = new Paging(results.size(), from, total - 1, results, null);
        ResponseEntity<String> responseEntity;
        if (total == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/resources/{resourceType}", params = {"from", "to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType, @RequestParam(value = "from") int from, @RequestParam(value = "to") int to) {
        // FIXME: very inefficient..
        //  create method returning the size of the results instead
        List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType), from, to);
        int total = resourceService.getResource(resourceTypeService.getResourceType(resourceType)).size();
        // -------------------------

        Paging paging = new Paging(results.size(), from, to, results, null);
        ResponseEntity<String> responseEntity;
        if (total == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/resources/{resourceType}", params = {"to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging> getResourceByResourceTypeTo(@PathVariable("resourceType") String resourceType, @RequestParam(value = "to") int to) {
        // FIXME: very inefficient..
        //  create method returning the size of the results instead
        List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType), 0, to);
        int total = resourceService.getResource(resourceTypeService.getResourceType(resourceType)).size();
        // -------------------------

        Paging paging = new Paging(results.size(), 0, to, results, null);
        ResponseEntity<String> responseEntity;
        if (total == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/resources/", params = {"from"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging> getAllResource(@RequestParam(value = "from") int from) {
        // FIXME: very inefficient..
        //  create method returning the size of the results instead
        List<Resource> results = resourceService.getResource(from, 0);
        int total = resourceService.getResource().size();
        // -------------------------

        Paging paging = new Paging(total, from, total - 1, results, null);
        ResponseEntity<String> responseEntity;
        if (total == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/resources/", params = {"from", "to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Paging> getAllResources(@RequestParam(value = "from") int from, @RequestParam(value = "to") int to) {
        // FIXME: very inefficient..
        //  create method returning the size of the results instead
        List<Resource> results = resourceService.getResource(from, to);
        int total = resourceService.getResource().size();
        // -------------------------

        Paging paging = new Paging(total, from, to, results, null);
        ResponseEntity<String> responseEntity;
        if (total == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }

    }

    @RequestMapping(value = "/resources/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> getAllResources() {

        StreamingResponseBody streamingResponseBody = outputStream -> {
            ObjectMapper mapper = new ObjectMapper();
            JsonGenerator g = mapper.getFactory().createGenerator(outputStream);
            g.writeStartObject();
            g.writeFieldName("results");
            g.writeStartArray();
            AtomicInteger totals = new AtomicInteger();
            resourceService.getResourceStream(r -> {
                try {
                    mapper.writeValue(g, r);
                    totals.getAndIncrement();
                } catch (IOException e) {
                    throw new ServiceException(e.getMessage());
                }
            });
            g.writeEndArray();
            g.writeObjectField("facets", null);
            g.writeNumberField("total", totals.intValue());
            g.writeNumberField("from", 0);
            g.writeNumberField("to", totals.intValue() - 1);
            g.writeEndObject();
            g.close();
        };

        return ResponseEntity
                .ok()
                .contentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
                .body(streamingResponseBody);
    }

    @RequestMapping(value = "/resources", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> addResource(@RequestBody Resource resource) {
        resourceService.addResource(resource);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/resource/{resourceId}/{resourceType}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
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
