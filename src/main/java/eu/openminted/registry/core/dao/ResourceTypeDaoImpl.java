package dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import domain.ResourceType;


@Repository("resourceTypeDao")
public class ResourceTypeDaoImpl extends AbstractDao<String, ResourceType> implements ResourceTypeDao{

	public ResourceType getResourceType(String name) {
		
		Criteria cr = getSession().createCriteria(ResourceType.class);
		cr.add(Restrictions.eq("name", name));
		if(cr.list().size()==0){
			return null;
		}else{
			return (ResourceType) cr.list().get(0);
		}
	
	}

	public List<ResourceType> getAllResourceType() {
		Criteria cr = getSession().createCriteria(ResourceType.class);
		return cr.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<ResourceType> getAllResourceType(int from, int to) {
		
		Criteria cr = getSession().createCriteria(ResourceType.class);
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
