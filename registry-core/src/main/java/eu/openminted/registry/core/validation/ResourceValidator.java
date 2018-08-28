package eu.openminted.registry.core.validation;

import eu.openminted.registry.core.dao.SchemaDao;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


/**
 * Created by stefanos on 30/1/2017.
 */
@Service("resourceValidator")
@Transactional
public class ResourceValidator {

    private static Logger logger = LogManager.getLogger(ResourceValidator.class);

    private SchemaDao schemaDao;

    @Autowired
    ResourceValidator(SchemaDao schemaDao) {
        this.schemaDao = schemaDao;
    }

    public boolean validateXML(String resourceType, String xmlContent) {
        Schema schema;
        try {
            schema = schemaDao.loadSchema(resourceType);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(IOUtils.toInputStream(xmlContent)));
        } catch (Exception e){
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    public boolean validateJSON(String resourceType, String jsonContent) {
        InputStream stream = new ByteArrayInputStream(resourceType.getBytes(StandardCharsets.UTF_8));
        JSONObject rawSchema = new JSONObject(new JSONTokener(stream));
        org.everit.json.schema.Schema schema = SchemaLoader.load(rawSchema);
        try{
            schema.validate(new JSONObject(jsonContent)); // throws a ValidationException if this object is invalid
        }catch(ValidationException e){
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

}
