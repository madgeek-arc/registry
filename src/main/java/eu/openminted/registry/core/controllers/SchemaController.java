package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SchemaController {

	private static Logger logger = Logger.getLogger(ResourceTypeController.class);

	@Autowired
	ResourceTypeService resourceTypeService;

	@RequestMapping(value = "/schemaService/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
	@ResponseBody public ResponseEntity getResourceTypeByName(@PathVariable("id") String id) throws ResourceNotFoundException {
		Schema schema = resourceTypeService.getSchema(id);
		if(schema==null){
			throw new ResourceNotFoundException();
		}else{
			return ResponseEntity.ok(schema.getSchema());
		}
	}

}
