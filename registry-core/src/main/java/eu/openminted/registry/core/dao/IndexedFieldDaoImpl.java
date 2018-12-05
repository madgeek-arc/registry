package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.index.IndexedField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("indexedFieldDao")
public class IndexedFieldDaoImpl extends AbstractDao<IndexedField> implements IndexedFieldDao {

    private static Logger logger = LogManager.getLogger(IndexedFieldDaoImpl.class);

    @Override
    public List<IndexedField> getIndexedFieldsOfResource(Resource resource) {
        return getList("resource", resource);
    }

    @Override
    public void deleteAllIndexedFields(Resource resource) {
        resource.getIndexedFields().forEach(iF ->{
            iF.setResource(null);
            persist(iF);
        });
    }


}
