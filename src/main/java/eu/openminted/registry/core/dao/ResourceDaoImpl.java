package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository("resourceDao")
public class ResourceDaoImpl extends AbstractDao<String, Resource> implements ResourceDao {

	public Resource getResource(String resourceType, String id) {

		Criteria cr = getSession().createCriteria(Resource.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.eq("id", id));

		if (resourceType != null)
			cr.add(Restrictions.eq("resourceType", resourceType));

		if (cr.list().size() == 0)
			return null;
		else
			return (Resource) cr.list().get(0);

	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(ResourceType resourceType) {

		Criteria cr = getSession().createCriteria(Resource.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.eq("resourceType", resourceType.getName()));

		return cr.list();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(ResourceType resourceType, int from, int to) {

		Criteria cr = getSession().createCriteria(Resource.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.eq("resourceType", resourceType.getName()));
		if (to == 0) {
			cr.setFirstResult(from);
		} else {
			int quantity;
			if(from==0)
				quantity = to;
			else
				quantity = to - from;
			cr.setFirstResult(from);
			cr.setMaxResults(quantity);
		}

		return cr.list();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(int from, int to) {

		Criteria cr = getSession().createCriteria(Resource.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		if (to == 0) {
			cr.setFirstResult(from);
		} else {
			int quantity = 10;
			if(from==0)
				quantity = to;
			else
				quantity = to - from;
			cr.setFirstResult(from);
			cr.setMaxResults(quantity);
		}
		return cr.list();
	}

	public List<Resource> getResource() {
		Criteria cr = getSession().createCriteria(Resource.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return cr.list();
	}


	public void addResource(Resource resource){
		persist(resource);
		getSession().flush();
	}

	public void updateResource(Resource resource) {
		resource.setModificationDate(new Date());
		getSession().merge(resource);
		getSession().flush();
	}

	public void deleteResource(String id) {
		Resource resource = getSession().get(Resource.class,id);
		getSession().delete(resource);
	}

}
