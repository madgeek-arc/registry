package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.SchemaDao;
import eu.openminted.registry.core.domain.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchemaListener implements ResourceTypeListener {

    private static final Logger logger = LoggerFactory.getLogger(SchemaListener.class);

    private final SchemaDao schemaDao;

    public SchemaListener(SchemaDao schemaDao) {
        this.schemaDao = schemaDao;
    }

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
            //do-nothing
    }

    @Override
    public void resourceTypeDelete(String name) {
        logger.info("Deleting schema with originalUrl: {}", name);
        schemaDao.deleteSchema(schemaDao.getSchemaByUrl(name));
    }
}
