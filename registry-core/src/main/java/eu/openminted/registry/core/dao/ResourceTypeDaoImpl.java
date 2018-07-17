package eu.openminted.registry.core.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository("resourceTypeDao")
public class ResourceTypeDaoImpl extends AbstractDao<String, ResourceType> implements ResourceTypeDao {

    private LoadingCache<String, Optional<ResourceType>> resourceTypeCacheLoader;

    public ResourceTypeDaoImpl() {
        super();
        CacheLoader<String, Optional<ResourceType>> loader;
        final ResourceTypeDaoImpl self = this;
        loader = new CacheLoader<String, Optional<ResourceType>>() {
            @Override
            public Optional<ResourceType> load(String name) {
                return Optional.ofNullable(self.getSession().get(ResourceType.class,name));
            }
        };
        resourceTypeCacheLoader = CacheBuilder.newBuilder().build(loader);
    }

	public ResourceType getResourceType(String name) {
        return resourceTypeCacheLoader.getUnchecked(name).orElse(null);
	}

	public List<ResourceType> getAllResourceType() {
		Criteria cr = getSession().createCriteria(ResourceType.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return cr.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<ResourceType> getAllResourceType(int from, int to) {
		
		Criteria cr = getSession().createCriteria(ResourceType.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		if(to==0){
			cr.setFirstResult(from);
		}else{
			cr.setFirstResult(from);
			cr.setMaxResults((to-from)+1);
		}
		return cr.list();
	}

	public void addResourceType(ResourceType resourceType) {
		persist(resourceType);
		getSession().flush();
	}

	@Override
	public Set<IndexField> getResourceTypeIndexFields(String name) {
		Set<IndexField> indexFields = new HashSet<>();
		Query query = getSession().createQuery("from IndexField where resourceType in " +
				"(from ResourceType where name = :name or aliasGroup = :name)");
		query.setParameter("name",name);
		indexFields.addAll(query.list());
		return indexFields;
	}

	@Override
	public void deleteResourceType(ResourceType resourceType) {
		getSession().delete(resourceType);
		getSession().flush();

		getSession().createSQLQuery("DROP VIEW "+resourceType.getName()+"_view;");
		getSession().flush();
	}

}
