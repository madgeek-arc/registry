package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("indexFieldDao")
public class IndexFieldDaoImpl extends AbstractDao<IndexField> implements IndexFieldDao {

    @Override
    public List<IndexField> getIndexFieldsOfResourceType(ResourceType resourceType) {
        return getList("resourceType", resourceType);
    }
}
