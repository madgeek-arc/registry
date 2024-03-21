package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository("resourceTypeDao")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        readOnly = true)
public class ResourceTypeDaoImpl extends AbstractDao<ResourceType> implements ResourceTypeDao {

    public ResourceTypeDaoImpl() {
        super();
    }

    public ResourceType getResourceType(String name) {
        return getSingleResult("name", name);
    }

    public List<ResourceType> getAllResourceType() {
        return getList();
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getAllResourceType(int from, int to) {

        CriteriaQuery<ResourceType> criteriaQuery = getCriteriaQuery();
        Root<ResourceType> root = criteriaQuery.from(ResourceType.class);
        criteriaQuery.distinct(true);
        criteriaQuery.select(root);

        TypedQuery<ResourceType> typedQuery = getEntityManager().createQuery(criteriaQuery);
        if (to == 0) {
            typedQuery.setFirstResult(from);
        } else {
            typedQuery.setFirstResult(from);
            typedQuery.setMaxResults((to - from) + 1);
        }
        return typedQuery.getResultList();
    }

    @Override
    @Transactional
    public void addResourceType(ResourceType resourceType) {
        super.persist(resourceType);
    }

    @Override
    public Set<IndexField> getResourceTypeIndexFields(String name) {
        Set<IndexField> indexFields = new HashSet<>();
        Query query = getEntityManager().createQuery("from IndexField where resourceType in " +
                "(from ResourceType where name = :name or aliasGroup = :name)");
        query.setParameter("name", name);
        indexFields.addAll(query.getResultList());
        return indexFields;
    }

    @Override
    @Transactional
    public void deleteResourceType(String resourceType) {
        super.delete(getResourceType(resourceType));
    }

}
