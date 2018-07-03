package eu.openminted.registry.core.views;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import eu.openminted.registry.core.service.ViewService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViewResourceTypeListener implements ResourceTypeListener{



    private static Logger logger = LogManager.getLogger(ViewResourceTypeListener.class);


    @Autowired
    ViewService viewService;

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        viewService.createView(resourceType);
    }

    @Override
    public void resourceTypeDelete(String name) {
        viewService.deleteView(name);
    }

}
