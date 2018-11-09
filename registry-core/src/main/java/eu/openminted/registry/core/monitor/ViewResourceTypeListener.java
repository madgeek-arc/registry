package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.dao.ViewsDao;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViewResourceTypeListener implements ResourceTypeListener{



    private static Logger logger = LogManager.getLogger(ViewResourceTypeListener.class);


    @Autowired
    ViewsDao viewsDao;

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        viewsDao.createView(resourceType);
    }

    @Override
    public void resourceTypeDelete(String name) {
        viewsDao.deleteView(name);
    }

}
