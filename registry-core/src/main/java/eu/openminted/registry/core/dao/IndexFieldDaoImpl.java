package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository("indexFieldDao")
public class IndexFieldDaoImpl extends AbstractDao<String, IndexField> implements IndexFieldDao {

	private static Logger logger = LogManager.getLogger(IndexFieldDaoImpl.class);


    @Override
    public List<IndexField> getIndexFieldsOfResourceType(ResourceType resourceType) {
        Criteria cr = getSession().createCriteria(IndexField.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        cr.add(Restrictions.eq("resourceType", resourceType));

        if (cr.list().size() == 0)
            return new ArrayList<>();
        else {
            return (List<IndexField>) cr.list();
        }
    }
}
