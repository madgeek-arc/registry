package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.Occurencies;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class ResourceTypeController {

	private static Logger logger = Logger.getLogger(ResourceTypeController.class);

	@Autowired
	ResourceTypeService resourceTypeService;

	@RequestMapping(value = "/resourceType/{name}", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypeByName(@PathVariable("name") String name) {
		ResponseEntity<String> responseEntity;
		ResourceType resourceType = resourceTypeService.getResourceType(name);
		if (resourceType == null) {
			responseEntity = new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		} else {
			responseEntity = new ResponseEntity<String>(Utils.objToJson(resourceType), HttpStatus.OK);
		}
		return responseEntity;
	}

	@RequestMapping(value = "/resourceType/", params = {"from"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypes(@RequestParam(value = "from") int from) {
		List<ResourceType> results = resourceTypeService.getAllResourceType(from, 0);
		Paging paging = new Paging(results.size(), 0, results.size() - 1, results,new Occurencies());
		ResponseEntity<String> responseEntity;
		if (results.size() == 0) {
			responseEntity = new ResponseEntity<String>(Utils.objToJson(paging), HttpStatus.NO_CONTENT);
		} else {
			responseEntity = new ResponseEntity<String>(Utils.objToJson(paging), HttpStatus.OK);
		}
		return responseEntity;

	}

	@RequestMapping(value = "/resourceType/", params = {"from", "to"}, method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypes(@RequestParam(value = "from") int from, @RequestParam(value = "from") int to) {
		List<ResourceType> results = resourceTypeService.getAllResourceType(from, to);
		int total = resourceTypeService.getAllResourceType().size();
		Paging paging = new Paging(total, from, to, results,new Occurencies());
		ResponseEntity<String> responseEntity;
		if (total == 0) {
			responseEntity = new ResponseEntity<String>(Utils.objToJson(paging), HttpStatus.NO_CONTENT);
		} else {
			responseEntity = new ResponseEntity<String>(Utils.objToJson(paging), HttpStatus.OK);
		}
		return responseEntity;
	}

	@RequestMapping(value = "/resourceType/", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<String> getResourceTypes() {
		List<ResourceType> results = resourceTypeService.getAllResourceType();
		Paging paging = new Paging(results.size(), 0, results.size() - 1, results,new Occurencies());
		ResponseEntity<String> responseEntity;

		if (results.size() == 0) {
			responseEntity = new ResponseEntity<String>(Utils.objToJson(paging), HttpStatus.NO_CONTENT);
		} else {
			responseEntity = new ResponseEntity<String>(Utils.objToJson(paging), HttpStatus.OK);
		}

		logger.info(responseEntity.toString());

		return responseEntity;
	}

	@RequestMapping(value = "/resourceType", method = RequestMethod.POST, headers = "Accept=application/json")
	public ResponseEntity<String> addResourceType(@RequestBody ResourceType resourceType) {
		ResponseEntity<String> responseEntity;
		
		resourceType.setCreationDate(new Date());
		resourceType.setModificationDate(new Date());
		responseEntity = new ResponseEntity<String>(Utils.objToJson(resourceType), HttpStatus.CREATED);
		try {
			resourceTypeService.addResourceType(resourceType);
		} catch (ServiceException e) {
			logger.error("Error saving resource type", e);
			responseEntity = new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return responseEntity;
	}
	

}
