package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.ViewDao;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViewResourceTypeListener implements ResourceTypeListener{



    private static Logger logger = LogManager.getLogger(ViewResourceTypeListener.class);


    @Autowired
    ViewDao viewDao;

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        viewDao.createView(resourceType);
    }

    @Override
    public void resourceTypeDelete(String name) {
        viewDao.deleteView(name);
    }

}
