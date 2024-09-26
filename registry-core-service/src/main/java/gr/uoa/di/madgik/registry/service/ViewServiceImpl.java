package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.dao.ViewDao;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("viewService")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class ViewServiceImpl implements ViewService {

    private final ViewDao viewDao;

    public ViewServiceImpl(ViewDao viewDao) {
        this.viewDao = viewDao;
    }

    @Override
    public void createView(ResourceType resourceType) {
        viewDao.createView(resourceType);
    }

    @Override
    public void deleteView(String resourceType) {
        viewDao.deleteView(resourceType);
    }

}

