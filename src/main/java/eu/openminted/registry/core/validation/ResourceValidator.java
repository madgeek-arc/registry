package eu.openminted.registry.core.validation;

import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;


/**
 * Created by stefanos on 30/1/2017.
 */
@Service("resourceValidator")
public class ResourceValidator {

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    @Autowired
    private ResourceTypeResolver resourceTypeResolver;

    public boolean validate(String resourceType, String xmlContent) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        try {
            DocumentBuilder parser = builderFactory
                    .newDocumentBuilder();

            // parse the XML into a document object
            Document document = parser.parse(IOUtils.toInputStream(xmlContent));

            SchemaFactory factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // associate the schema factory with the resource resolver, which is responsible for resolving the imported XSD's
            factory.setResourceResolver(resourceTypeResolver);

            // note that if your XML already declares the XSD to which it has to conform, then there's no need to create a validator from a Schema object
            ResourceType resourceTypeXsd = resourceTypeDao.getResourceType(resourceType);
            InputStream streamXsd = IOUtils.toInputStream(resourceTypeXsd.getSchema(), "UTF-8");
            Source schemaFile = new StreamSource(streamXsd);
            Schema schema = factory.newSchema(schemaFile);

            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));

        } catch (Exception e){
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

}
