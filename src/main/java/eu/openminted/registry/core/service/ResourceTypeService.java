package service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dao.ResourceTypeDao;
import domain.ResourceType;

@Service("resourceTypeService")
@Transactional
public class ResourceTypeService {
	  
	@Autowired
	ResourceTypeDao resourceTypeDao;  
	
	 public ResourceTypeService() {  
	 
	 }  
	  
	 public ResourceType getResourceType(String name){  
		 ResourceType resourceType = resourceTypeDao.getResourceType(name);  
		 return resourceType;  
	 } 
	 
	 public List<ResourceType> getAllResourceType(){  
		 List<ResourceType> resourceType = resourceTypeDao.getAllResourceType();  
		 return resourceType;  
	 } 
	 
	 public List<ResourceType> getAllResourceType(int from, int to){  
		 List<ResourceType> resourceType = resourceTypeDao.getAllResourceType(from,to);  
		 return resourceType;  
	 } 
	 
	 public ResourceType addResourceType(ResourceType resourceType){  
		 resourceTypeDao.addResourceType(resourceType);
		 return resourceType;
	 }  
	   
	 public ResourceTypeDao getResourceTypeDao() {
		return resourceTypeDao;
	 }

	 public void setResourceTypeDao(ResourceTypeDao resourceTypeDao) {
		this.resourceTypeDao = resourceTypeDao;
     }  
	  
}
