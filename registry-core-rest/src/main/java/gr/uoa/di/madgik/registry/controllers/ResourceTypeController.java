package gr.uoa.di.madgik.registry.controllers;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.IndexFieldService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
public class ResourceTypeController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeController.class);

    private final ResourceTypeService resourceTypeService;
    private final IndexFieldService indexFieldService;

    public ResourceTypeController(ResourceTypeService resourceTypeService, IndexFieldService indexFieldService) {
        this.resourceTypeService = resourceTypeService;
        this.indexFieldService = indexFieldService;
    }

    @RequestMapping(value = "/resourceType/index/{name}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity getResourceTypeIndexFields(@PathVariable("name") String name) {
        return new ResponseEntity(indexFieldService.getIndexFields(name), HttpStatus.OK);
    }

    @RequestMapping(value = "/resourceType/{name}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<ResourceType> getResourceTypeByName(@PathVariable("name") String name) throws ResourceNotFoundException {
        ResourceType resourceType = resourceTypeService.getResourceType(name);
        if (resourceType == null) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(resourceType, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/resourceType/", params = {"from"}, method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<Paging> getResourceTypes(@RequestParam(value = "from") int from) throws ResourceNotFoundException {
        List<ResourceType> results = resourceTypeService.getAllResourceType(from, 0);
        Paging paging = new Paging<>(results.size(), 0, results.size() - 1, results, null);
        if (results.size() == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/resourceType/", params = {"from", "to"}, method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<Paging> getResourceTypes(@RequestParam(value = "from") int from, @RequestParam(value = "from") int to) throws ResourceNotFoundException {
        List<ResourceType> results = resourceTypeService.getAllResourceType(from, to);
        int total = resourceTypeService.getAllResourceType().size();
        Paging paging = new Paging<>(total, from, to, results, null);
        if (total == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/resourceType/", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<Paging> getResourceTypes() throws ResourceNotFoundException {
        List<ResourceType> results = resourceTypeService.getAllResourceType();
        Paging paging = new Paging<>(results.size(), 0, results.size() - 1, results, null);

        if (results.size() == 0) {
            throw new ResourceNotFoundException();
        } else {
            return new ResponseEntity<>(paging, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/resourceType", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<ResourceType> addResourceType(@RequestBody ResourceType resourceType) {
        resourceType.setCreationDate(new Date());
        resourceType.setModificationDate(new Date());
        try {
            resourceTypeService.addResourceType(resourceType);
            return new ResponseEntity<>(resourceType, HttpStatus.CREATED);
        } catch (ServiceException e) {
            logger.error("Error saving resource type", e);
            throw new ServiceException(e);
        }
    }

    @RequestMapping(value = "/resourceType/{name}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<ResourceType> deleteResourceType(@PathVariable("name") String name) {

        try {
            resourceTypeService.deleteResourceType(name);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ServiceException e) {
            logger.error("Error deleting resource type", e);
            throw new ServiceException(e);
        }
    }


}
