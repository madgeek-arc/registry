package eu.openminted.registry.core.monitor;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViewResourceTypeListener implements ResourceTypeListener{



    private static Logger logger = LoggerFactory.getLogger(ViewResourceTypeListener.class);


    @Autowired
    private ViewService viewService;

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        viewService.createView(resourceType);
    }

    @Override
    public void resourceTypeDelete(String name) {
        viewService.deleteView(name);
    }

}
