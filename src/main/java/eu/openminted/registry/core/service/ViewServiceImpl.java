package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.ViewsDaoImpl;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("viewService")
@Transactional
public class ViewServiceImpl implements ViewService {

    private static Logger logger = LogManager.getLogger(ViewServiceImpl.class);


    @Autowired
    ViewsDaoImpl viewsDao;


    @Override
    public void createView(ResourceType resourceType) {
        viewsDao.createView(resourceType);
    }

    @Override
    public void deleteView(String resourceType) {
        viewsDao.deleteView(resourceType);
    }

}

