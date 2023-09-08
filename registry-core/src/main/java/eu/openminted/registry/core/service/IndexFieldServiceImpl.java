package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.IndexFieldDao;
import eu.openminted.registry.core.domain.index.IndexField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("indexFieldService")
public class IndexFieldServiceImpl implements IndexFieldService {

    private static Logger logger = LoggerFactory.getLogger(IndexFieldServiceImpl.class);

    private final IndexFieldDao indexFieldDao;
    private final ResourceTypeService resourceTypeService;

    public IndexFieldServiceImpl(IndexFieldDao indexFieldDao, ResourceTypeService resourceTypeService) {
        this.indexFieldDao = indexFieldDao;
        this.resourceTypeService = resourceTypeService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndexField> getIndexFields(String resourceTypeName) {
        return indexFieldDao.getIndexFieldsOfResourceType(resourceTypeService.getResourceType(resourceTypeName));
    }
}

