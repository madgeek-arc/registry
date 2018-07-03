package eu.openminted.registry.core.validation;

import eu.openminted.registry.core.dao.SchemaDao;
import eu.openminted.registry.core.domain.Schema;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Created by stefanos on 30/1/2017.
 */
@Service("resourceTypeResolver")
public class ResourceTypeResolver implements LSResourceResolver {

    @Autowired
    SchemaDao schemaDao;

    @Override
    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId, String baseURI) {
        int index= systemId.lastIndexOf('/') + 1;
        String schemaId = systemId.substring(index);

        Schema schema = schemaDao.getSchema(schemaId);
        LSInput ret = new SchemaInput(publicId,schemaId, IOUtils.toInputStream(schema.getSchema()));
        return ret;
    }
}
