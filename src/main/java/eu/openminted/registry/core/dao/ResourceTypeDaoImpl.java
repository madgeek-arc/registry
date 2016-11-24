package eu.openminted.registry.core.dao;

import java.util.List;

import eu.openminted.registry.core.service.ServiceException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import eu.openminted.registry.core.domain.ResourceType;

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

}
