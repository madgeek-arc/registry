package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.index.IndexedField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository("indexedFieldDao")
public class IndexedFieldDaoImpl extends AbstractDao<String, IndexedField> implements IndexedFieldDao {

	private static Logger logger = LogManager.getLogger(IndexedFieldDaoImpl.class);


    @Override
    public List<IndexedField> getIndexedFieldsOfResource(Resource resource) {

        Criteria cr = getSession().createCriteria(IndexedField.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        cr.add(Restrictions.eq("resource", resource));

        if (cr.list().size() == 0)
            return new ArrayList<>();
        else {
            return (List<IndexedField>) cr.list();
        }

    }

}
