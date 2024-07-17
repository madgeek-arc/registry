package gr.uoa.di.madgik.registry.controllers;

import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SchemaController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeController.class);

    @Autowired
    ResourceTypeService resourceTypeService;

    @RequestMapping(value = "/schemaService/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public ResponseEntity getSchemaByName(@PathVariable("id") String id) throws ResourceNotFoundException {
        Schema schema = resourceTypeService.getSchema(id);
        if (schema == null) {
            throw new ResourceNotFoundException();
        } else {
            return ResponseEntity.ok(schema.getSchema());
        }
    }

}
