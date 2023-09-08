package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.IndexedFieldDao;
import eu.openminted.registry.core.domain.index.IndexedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("indexedFieldService")
public class IndexedFieldServiceImpl implements IndexedFieldService {

    private static Logger logger = LoggerFactory.getLogger(IndexedFieldServiceImpl.class);

    private final IndexedFieldDao indexedFieldDao;
    private final ResourceService resourceService;

    public IndexedFieldServiceImpl(IndexedFieldDao indexedFieldDao, ResourceService resourceService) {
        this.indexedFieldDao = indexedFieldDao;
        this.resourceService = resourceService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndexedField> getIndexedFields(String resourceId) {
        return indexedFieldDao.getIndexedFieldsOfResource(resourceService.getResource(resourceId));
    }

}

