package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Tools;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("resourceDao")
public class ResourceDaoImpl extends AbstractDao<String, Resource> implements ResourceDao {


	public Resource getResource(String resourceType, String id) {

		Criteria cr = getSession().createCriteria(Resource.class);
		cr.add(Restrictions.eq("id", id));

		if (resourceType != null)
			cr.add(Restrictions.eq("resourceType", resourceType));

		if (cr.list().size() == 0)
			return null;
		else
			return (Resource) cr.list().get(0);

	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(String resourceType) {

		Criteria cr = getSession().createCriteria(Resource.class);
		cr.add(Restrictions.eq("resourceType", resourceType));

		return cr.list();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(String resourceType, int from, int to) {

		Criteria cr = getSession().createCriteria(Resource.class);
		cr.add(Restrictions.eq("resourceType", resourceType));
		if (to == 0) {
			cr.setFirstResult(from);
		} else {
			cr.setFirstResult(from);
			cr.setMaxResults((to - from) + 1);
		}

		return cr.list();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(int from, int to) {

		Criteria cr = getSession().createCriteria(Resource.class);
		if (to == 0) {
			cr.setFirstResult(from);
		} else {
			cr.setFirstResult(from);
			cr.setMaxResults((to - from) + 1);
		}
		return cr.list();
	}

	public List<Resource> getResource() {

		Criteria cr = getSession().createCriteria(Resource.class);

		return cr.list();
	}


	public void addResource(Resource resource){
		persist(resource);
	}

	public void updateResource(Resource resource) {
		Session session = getSession();
		session.update(resource);

	}

	public void deleteResource(String id) {
		Query query = getSession().createSQLQuery("delete from resource where id = :id");
		query.setString("id", id);
		query.executeUpdate();
	}

}
