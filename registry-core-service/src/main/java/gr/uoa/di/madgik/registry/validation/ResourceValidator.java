package gr.uoa.di.madgik.registry.validation;

import gr.uoa.di.madgik.registry.dao.SchemaDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;


/**
 * Created by stefanos on 30/1/2017.
 */
@Service("resourceValidator")
public class ResourceValidator {

    private static Logger logger = LoggerFactory.getLogger(ResourceValidator.class);

    private SchemaDao schemaDao;

    @Autowired
    ResourceValidator(SchemaDao schemaDao) {
        this.schemaDao = schemaDao;
    }

    public boolean validateXML(Resource resource) {
        Schema schema;
        try {
            schema = schemaDao.loadXMLSchema(resource.getResourceType());
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(IOUtils.toInputStream(resource.getPayload())));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    public boolean validateJSON(Resource resource) {
        org.everit.json.schema.Schema schema = schemaDao.loadJSONSchema(resource.getResourceType());
        try {
            schema.validate(new JSONObject(resource.getPayload())); // throws a ValidationException if this object is invalid
        } catch (ValidationException e) {
            logger.error("Error validation JSON payload", e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

}
