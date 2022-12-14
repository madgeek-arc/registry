package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository("indexFieldDao")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class IndexFieldDaoImpl extends AbstractDao<IndexField> implements IndexFieldDao {

    @Override
    public List<IndexField> getIndexFieldsOfResourceType(ResourceType resourceType) {
        return getList("resourceType",resourceType);
    }
}
