package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.IndexFieldDao;
import eu.openminted.registry.core.domain.index.IndexField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("indexFieldService")
@Transactional
public class IndexFieldServiceImpl implements IndexFieldService{

    private static Logger logger = LogManager.getLogger(IndexFieldServiceImpl.class);

    @Autowired
    IndexFieldDao indexFieldDao;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Override
    public List<IndexField> getIndexFields(String resourceTypeName) {
        return indexFieldDao.getIndexFieldsOfResourceType(resourceTypeService.getResourceType(resourceTypeName));
    }

    @Override
    public IndexField getIndexField(String indexFieldName) {
        return indexFieldDao.getIndexField(indexFieldName);
    }

    @Override
    public IndexField add(IndexField indexField) {
        indexFieldDao.add(indexField);
        indexField.getResourceType().getIndexFields().add(indexField);
        return indexField;
    }

    @Override
    public void delete(String indexField,String resourceTypeName) {
        IndexField indexFieldObj = getIndexField(indexField);
        if(indexFieldObj==null)
            throw new ServiceException("Index field not found");

        resourceTypeService.getResourceType(resourceTypeName).getIndexFields().remove(indexFieldObj);
        indexFieldDao.remove(indexFieldObj);
    }
}

