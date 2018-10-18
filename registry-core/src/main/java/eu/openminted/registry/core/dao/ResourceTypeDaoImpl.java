package eu.openminted.registry.core.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository("resourceTypeDao")
@Transactional
public class ResourceTypeDaoImpl extends AbstractDao<ResourceType> implements ResourceTypeDao {

    private LoadingCache<String, Optional<ResourceType>> resourceTypeCacheLoader;

    public ResourceTypeDaoImpl() {
        super();
        CacheLoader<String, Optional<ResourceType>> loader;
        final ResourceTypeDaoImpl self = this;
        loader = new CacheLoader<String, Optional<ResourceType>>() {
            @Override
            public Optional<ResourceType> load(String name) {
                return Optional.ofNullable(getEntityManager().find(ResourceType.class,name));
            }
        };
        resourceTypeCacheLoader = CacheBuilder.newBuilder().build(loader);
    }

	public ResourceType getResourceType(String name) {
        return resourceTypeCacheLoader.getUnchecked(name).orElse(null);
	}

	public List<ResourceType> getAllResourceType() {
		return getList();
	}
	
	@SuppressWarnings("unchecked")
	public List<ResourceType> getAllResourceType(int from, int to) {

		criteriaQuery = getCriteriaQuery();
		root = criteriaQuery.from(ResourceType.class);
		criteriaQuery.distinct(true);
		criteriaQuery.select(root);

		TypedQuery<ResourceType> typedQuery = getEntityManager().createQuery(criteriaQuery);
		if (to == 0) {
			typedQuery.setFirstResult(from);
		} else {
			typedQuery.setFirstResult(from);
			typedQuery.setMaxResults((to-from)+1);
		}
		return typedQuery.getResultList();
	}

	public void addResourceType(ResourceType resourceType) {
		persist(resourceType);
		resourceTypeCacheLoader.refresh(resourceType.getName());
	}

	@Override
	public Set<IndexField> getResourceTypeIndexFields(String name) {
		Set<IndexField> indexFields = new HashSet<>();
		Query query = getEntityManager().createQuery("from IndexField where resourceType in " +
				"(from ResourceType where name = :name or aliasGroup = :name)");
		query.setParameter("name",name);
		indexFields.addAll(query.getResultList());
		return indexFields;
	}

	@Override
	public void deleteResourceType(ResourceType resourceType) {
        resourceTypeCacheLoader.invalidate(resourceType.getName());
		delete(resourceType);
	}

}
