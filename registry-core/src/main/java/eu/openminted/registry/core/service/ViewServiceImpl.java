package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.ViewDao;
import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("viewService")
public class ViewServiceImpl implements ViewService {

    @Autowired
    ViewDao viewDao;


    @Override
    public void createView(ResourceType resourceType) {
        viewDao.createView(resourceType);
    }

    @Override
    public void deleteView(String resourceType) {
        viewDao.deleteView(resourceType);
    }

}

