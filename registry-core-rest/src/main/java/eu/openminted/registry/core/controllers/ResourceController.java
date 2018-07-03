package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@Transactional
public class ResourceController {

	   @Autowired
	   ResourceService resourceService;

	   @Autowired
	   ResourceTypeService resourceTypeService;
	  
	    @RequestMapping(value = "/resources/{resourceType}/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE )
	    public ResponseEntity<Resource> getResourceById(@PathVariable("resourceType") String resourceType,@PathVariable("id") String id) throws ResourceNotFoundException {
	    	Resource resource = resourceService.getResource(resourceTypeService.getResourceType(resourceType),id);
	    	if(resource==null){
				throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(resource, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType) throws ResourceNotFoundException {
	        List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType));
	    	Paging paging = new Paging(results.size(), 0, results.size()-1, results,null);
	    	if(results.size()==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"from"},method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType ,@RequestParam(value = "from") int from) throws ResourceNotFoundException {
	    	List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType),from,0);
	    	int total = resourceService.getResource(resourceTypeService.getResourceType(resourceType)).size();
	    	Paging paging = new Paging(results.size(), from, total-1, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    }
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"from","to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType ,@RequestParam(value = "from") int from , @RequestParam(value = "to") int to ) throws ResourceNotFoundException {
	    	List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType),from,to);
	    	int total = resourceService.getResource(resourceTypeService.getResourceType(resourceType)).size();
	    	Paging paging = new Paging(results.size(), from, to, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Paging> getResourceByResourceTypeTo(@PathVariable("resourceType") String resourceType , @RequestParam(value = "to") int to ) throws ResourceNotFoundException {
	    	List<Resource> results = resourceService.getResource(resourceTypeService.getResourceType(resourceType),0,to);
	    	int total = resourceService.getResource(resourceTypeService.getResourceType(resourceType)).size();
	    	Paging paging = new Paging(results.size(), 0, to, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/",params = {"from"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Paging> getAllResource(@RequestParam(value = "from") int from ) throws ResourceNotFoundException {
	    	List<Resource> results = resourceService.getResource(from,0);
	    	int total = resourceService.getResource().size();
	    	Paging paging = new Paging(total, from, total-1, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    }
	    
	    @RequestMapping(value = "/resources/",params = {"from","to"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Paging> getAllResources(@RequestParam(value = "from") int from , @RequestParam(value = "to") int to) throws ResourceNotFoundException {
	    	List<Resource> results = resourceService.getResource(from,to);
	    	int total = resourceService.getResource().size();
	    	Paging paging = new Paging(total, from, to, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    }
	    
	    @RequestMapping(value = "/resources/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Paging> getAllResources() throws ResourceNotFoundException {
	    	List<Resource> results = resourceService.getResource();
	    	int total = resourceService.getResource().size();
	    	Paging paging = new Paging(total, 0, total-1, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    	
	    } 
	  
	    @RequestMapping(value = "/resources", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Resource> addResource(@RequestBody Resource resource) {
			resourceService.addResource(resource);
			return new ResponseEntity<>(resource, HttpStatus.CREATED);
	    }  
	  
	    @RequestMapping(value = "/resources", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public ResponseEntity<Resource> updateResource(@RequestBody Resource resource) {
	    	resource.setModificationDate(new Date());
	    	Resource resourceFinal;
			resourceFinal = resourceService.updateResource(resource);
	    	return new ResponseEntity<>(resourceFinal, HttpStatus.NO_CONTENT);
	    }  
	  
	    @RequestMapping(value = "/resources/{id}", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	    public void deleteResources(@PathVariable("id") String id) {  
	        resourceService.deleteResource(id);  
	    }   
	    
	  
  
}
