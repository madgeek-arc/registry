package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository("resourceTypeDao")
public class ResourceTypeDaoImpl extends AbstractDao<String, ResourceType> implements ResourceTypeDao {

	public ResourceType getResourceType(String name) {
		
		Criteria cr = getSession().createCriteria(ResourceType.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.eq("name", name));
		if(cr.list().size()==0){
			return null;
		}else{
			return (ResourceType) cr.list().get(0);
		}
	
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
	}

	@Override
	public Set<IndexField> getResourceTypeIndexFields(String name) {
		Set<IndexField> indexFields = new HashSet<>();
		Criteria cr = getSession().createCriteria(ResourceType.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.or(Restrictions.eq("name", name),Restrictions.eq("aliasGroup", name)));
		for(Object type : cr.list()) {
			indexFields.addAll(((ResourceType) type).getIndexFields());
		}
		return indexFields;
	}

	@Override
	public void deleteResourceType(ResourceType resourceType) {
		getSession().delete(resourceType);
		getSession().flush();
	}

}
