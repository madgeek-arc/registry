package eu.openminted.registry.core.controllers;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.domain.Tools;
import eu.openminted.registry.core.service.ResourceTypeService;

@RestController
public class SchemaController {

	private static Logger logger = Logger.getLogger(ResourceTypeController.class);

	@Autowired
	ResourceTypeService resourceTypeService;

	@RequestMapping(value = "/schemaService/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
	public String getResourceTypeByName(@PathVariable("id") String id) {
		ResponseEntity<String> responseEntity;
		Schema schema = resourceTypeService.getSchema(id);
		if (schema == null) {
			responseEntity = new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		} else {
			responseEntity = new ResponseEntity<String>(Tools.objToJson(schema), HttpStatus.OK);
		}
//		return responseEntity;
		if(schema==null){
			return "";
		}else{
			return schema.getSchema();
		}
//		SolrClass solrClass = new SolrClass();
//		return solrClass.SolrClass().toString();
	}
}
