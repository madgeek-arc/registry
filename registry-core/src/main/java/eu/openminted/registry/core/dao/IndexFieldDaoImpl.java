package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("indexFieldDao")
public class IndexFieldDaoImpl extends AbstractDao<IndexField> implements IndexFieldDao {

    @Override
    public List<IndexField> getIndexFieldsOfResourceType(ResourceType resourceType) {
        return getList("resourceType",resourceType);
    }
}
