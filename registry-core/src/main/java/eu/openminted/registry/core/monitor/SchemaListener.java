package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.SchemaDao;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchemaListener implements ResourceTypeListener {

    @Autowired
    SchemaDao schemaDao;

    private static Logger logger = LogManager.getLogger(SchemaListener.class);

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
            //do-nothing
    }

    @Override
    public void resourceTypeDelete(String name) {
        logger.info("Deleting schema with originalUrl" + name);
        schemaDao.deleteSchema(schemaDao.getSchemaByUrl(name));
    }
}
