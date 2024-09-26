package gr.uoa.di.madgik.registry.monitor;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ViewResourceTypeListener implements ResourceTypeListener {

    private static final Logger logger = LoggerFactory.getLogger(ViewResourceTypeListener.class);

    private final ViewService viewService;

    public ViewResourceTypeListener(ViewService viewService) {
        this.viewService = viewService;
    }

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        viewService.createView(resourceType);
    }

    @Override
    public void resourceTypeDelete(String name) {
        viewService.deleteView(name);
    }

}
