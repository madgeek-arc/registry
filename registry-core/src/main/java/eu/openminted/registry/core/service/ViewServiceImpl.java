package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.ViewsDao;
import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("viewService")
@Transactional
public class ViewServiceImpl implements ViewService {

    @Autowired
    ViewsDao viewsDao;


    @Override
    public void createView(ResourceType resourceType) {
        viewsDao.createView(resourceType);
    }

    @Override
    public void deleteView(String resourceType) {
        viewsDao.deleteView(resourceType);
    }

}

