package service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import classes.Resource;
import dao.ResourceDao;

@Service("resourceService")
@Transactional
public class ResourceService {
	  
	@Autowired
	ResourceDao resourceDao;  
	
	 public ResourceService() {  
	 
	 }  
	  
	 public Resource getResource(String resourceType,String id){  
		 Resource resource = resourceDao.getResource(resourceType,id);  
		 return resource;  
	 } 
	 
	 public List<Resource> getResource(String resourceType){  
		 List<Resource> resources = resourceDao.getResource(resourceType);  
		 return resources;  
	 } 
	 
	 public List<Resource> getResource(String resourceType, int from, int to){  
		 List<Resource> resources = resourceDao.getResource(resourceType ,from ,to);  
		 return resources;  
	 } 
	 
	 public List<Resource> getResource(int from, int to){  
		 List<Resource> resources = resourceDao.getResource(from ,to);  
		 return resources;  
	 } 
	 
	 public List<Resource> getResource(){  
		 List<Resource> resources = resourceDao.getResource();  
		 return resources;  
	 } 
	 
	 public String addResource(Resource resource){  
		 String response = resourceDao.addResource(resource);
		 return response;
	 }  
	   
	 public Resource updateResource(Resource resource){  
	    resourceDao.updateResource(resource);
	    return resource;
	 }
	 
	 public void deleteResource(String id){  
		 resourceDao.deleteResource(id);
	 }

	 public ResourceDao getResourceDao() {
		return resourceDao;
	 }

	 public void setResourceDao(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
     }  
	  
}
