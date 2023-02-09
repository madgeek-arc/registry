package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.IndexedFieldDao;
import eu.openminted.registry.core.domain.index.IndexedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("indexedFieldService")
@Transactional
public class IndexedFieldServiceImpl implements IndexedFieldService {

    private static Logger logger = LoggerFactory.getLogger(IndexedFieldServiceImpl.class);

    @Autowired
    IndexedFieldDao indexedFieldDao;

    @Autowired
    ResourceService resourceService;

    @Override
    public List<IndexedField> getIndexedFields(String resourceId) {
        return indexedFieldDao.getIndexedFieldsOfResource(resourceService.getResource(resourceId));
    }

}

