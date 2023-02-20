package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.index.IndexedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository("indexedFieldDao")
public class IndexedFieldDaoImpl extends AbstractDao<IndexedField> implements IndexedFieldDao {

    private static final Logger logger = LoggerFactory.getLogger(IndexedFieldDaoImpl.class);

    @Override
    @Transactional(readOnly = true)
    public List<IndexedField> getIndexedFieldsOfResource(Resource resource) {
        return getList("resource", resource);
    }

    @Override
    @Transactional
    public void deleteAllIndexedFields(Resource resource) {
        resource.getIndexedFields().forEach(iF -> {
            iF.setResource(null);
//            persist(iF);
            getEntityManager().persist(iF);
            getEntityManager().flush();
        });
    }


}
