package eu.openminted.registry.core.service;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.index.DefaultIndexMapper;

@Service("resourceTypeService")
@Transactional
public class ResourceTypeService {

	private static Logger logger = Logger.getLogger(ResourceTypeService.class);
	  
	@Autowired
	ResourceTypeDao resourceTypeDao;
	
	 public ResourceTypeService() {  
	 
	 }  
	  
	 public ResourceType getResourceType(String name){  
		 ResourceType resourceType = resourceTypeDao.getResourceType(name);
		 return resourceType;  
	 } 
	 
	 public List<ResourceType> getAllResourceType() {
		 List<ResourceType> resourceType = resourceTypeDao.getAllResourceType();

		 return resourceType;  
	 } 
	 
	 public List<ResourceType> getAllResourceType(int from, int to){  
		 List<ResourceType> resourceType = resourceTypeDao.getAllResourceType(from,to);
		 return resourceType;  
	 } 
	 
	 public ResourceType addResourceType(ResourceType resourceType){

		 if (resourceType.getIndexMapperClass() == null)
			 resourceType.setIndexMapperClass(DefaultIndexMapper.class.getName());

		 if (resourceType.getIndexFields() != null) {
			 for (IndexField field:resourceType.getIndexFields())
				 field.setResourceType(resourceType);
		 }

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
