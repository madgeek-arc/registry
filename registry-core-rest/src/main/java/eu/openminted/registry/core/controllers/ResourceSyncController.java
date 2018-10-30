package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.service.ResourceSyncService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	private static Logger logger = LogManager.getLogger(ResourceSyncController.class);

	@Autowired
	ResourceSyncService resourceSyncService;


	@RequestMapping(value = "/resourcesync/{name}/resourcelist.xml", method = RequestMethod.GET, headers = "Accept=application/xml")
	public ResponseEntity getResourceListController(@PathVariable("name") String name){
		return new ResponseEntity(resourceSyncService.getResourceList(name).serialise(),HttpStatus.OK);
	}

	@RequestMapping(value = "/resourcesync/", method = RequestMethod.GET, headers = "Accept=application/xml")
	public ResponseEntity getCapabilityListController(){
		return new ResponseEntity(resourceSyncService.getCapabilityList().serialise(),HttpStatus.OK);
	}

	@RequestMapping(value = "/resourcesync/{resourceType}/{date}/changelist.xml", method = RequestMethod.GET, headers = "Accept=application/xml")
	public ResponseEntity getChangeListController(@PathVariable("resourceType") String resourceType, @PathVariable("date") Long date){
		return new ResponseEntity(resourceSyncService.getChangeList(resourceType, new Date(date)).serialise(),HttpStatus.OK);
	}

}
