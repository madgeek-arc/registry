package dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import classes.Resource;
import classes.ResourceType;
import classes.Tools;


@Repository("resourceDao")
public class ResourceDaoImpl extends AbstractDao<String, Resource> implements ResourceDao{

	@Autowired
	ResourceTypeDao resourceTypeDao;
	
	public Resource getResource(String resourceType,String id) {
		
		Criteria cr = getSession().createCriteria(Resource.class);
		cr.add(Restrictions.eq("id", id));
		cr.add(Restrictions.eq("resourceType",resourceType));
		
		if(cr.list().size()==0)
			return null;
		else
			return (Resource) cr.list().get(0);
		
	}
	
	@SuppressWarnings("unchecked")
	public List<Resource> getResource(String resourceType) {
		
		Criteria cr = getSession().createCriteria(Resource.class);
		cr.add(Restrictions.eq("resourceType",resourceType));
		
		return cr.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<Resource> getResource(String resourceType, int from ,int to) {
		
		Criteria cr = getSession().createCriteria(Resource.class);
		cr.add(Restrictions.eq("resourceType",resourceType));
		if(to==0){
			cr.setFirstResult(from);
		}else{
			cr.setFirstResult(from);
			cr.setMaxResults((to-from)+1);
		}
		
		return cr.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<Resource> getResource(int from ,int to) {
		
		Criteria cr = getSession().createCriteria(Resource.class);
		if(to==0){
			cr.setFirstResult(from);
		}else{
			cr.setFirstResult(from);
			cr.setMaxResults((to-from)+1);
		}
		return cr.list();
	}

	public List<Resource> getResource() {
		
		Criteria cr = getSession().createCriteria(Resource.class);
		
		return cr.list();
	}

	
	public String addResource(Resource resource) {
		String response ="";
		ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType());
		if(resourceType!=null){
			if(resourceType.getPayloadType().equals(resource.getPayloadFormat())){
				if(resourceType.getPayloadType().equals("xml")){
					//validate xml
					String output = Tools.validateXMLSchema(resourceType.getSchema(), resource.getPayload());
					if(output.equals("true")){
						resource.setPayload(resource.getPayload());
						persist(resource);
						response = "OK";
					}else{
						response = "XML and XSD mismatch";
					}
				}else if(resourceType.getPayloadType().equals("json")){
					//validate json
					String jsonResponse = Tools.validateJSONSchema(resourceType.getSchema(), resource.getPayload());
					if(jsonResponse.equals("true")){
						resource.setPayload(resource.getPayload());
						persist(resource);
						response = "OK";
					}else{
						response = "JSON and Schema missmatch";
					}
				}else{
					//payload type not supported
					response = "type not supported";
				}
			}else{
				//payload and schema format do not match, we cant validate
				response = "payload and schema format are different";
			}
		}else{
			//resource type not found
			response = "resource type not found";
		}
		return response;
		
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
