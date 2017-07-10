package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchemaController {

	private static Logger logger = Logger.getLogger(ResourceTypeController.class);

	@Autowired
	ResourceTypeService resourceTypeService;

	@RequestMapping(value = "/schemaService/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
	public String getResourceTypeByName(@PathVariable("id") String id) {
		Schema schema = resourceTypeService.getSchema(id);
		if(schema==null){
			return "";
		}else{
			return schema.getSchema();
		}
	}
}
