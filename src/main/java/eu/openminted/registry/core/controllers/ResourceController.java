package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@Transactional
public class ResourceController {

	   @Autowired
	   ResourceService resourceService;
	  
	    @RequestMapping(value = "/resources/{resourceType}/{id}", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Resource> getResourceById(@PathVariable("resourceType") String resourceType,@PathVariable("id") String id) {  
	    	Resource resource = resourceService.getResource(resourceType,id);
	    	if(resource==null){
				throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(resource, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType) {
	        List<Resource> results = resourceService.getResource(resourceType);
	    	Paging paging = new Paging(results.size(), 0, results.size()-1, results,null);
	    	if(results.size()==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"from"},method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType ,@RequestParam(value = "from") int from) {
	    	List<Resource> results = resourceService.getResource(resourceType,from,0);
	    	int total = resourceService.getResource(resourceType).size();
	    	Paging paging = new Paging(results.size(), from, total-1, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    }
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"from","to"}, method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Paging> getResourceByResourceType(@PathVariable("resourceType") String resourceType ,@RequestParam(value = "from") int from , @RequestParam(value = "to") int to ) {
	    	List<Resource> results = resourceService.getResource(resourceType,from,to);
	    	int total = resourceService.getResource(resourceType).size();
	    	Paging paging = new Paging(results.size(), from, to, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"to"}, method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Paging> getResourceByResourceTypeTo(@PathVariable("resourceType") String resourceType , @RequestParam(value = "to") int to ) {
	    	List<Resource> results = resourceService.getResource(resourceType,0,to);
	    	int total = resourceService.getResource(resourceType).size();
	    	Paging paging = new Paging(results.size(), 0, to, results,null);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		throw new ResourceNotFoundException();
	    	}else{
	    		return new ResponseEntity<>(paging, HttpStatus.OK);
	    	}
	    	
	    } 
	    
	    @RequestMapping(value = "/resources/",params = {"from"}, method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Paging> getResourceByResourceType(@RequestParam(value = "from") int from ) {
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
	    
	    @RequestMapping(value = "/resources/",params = {"from","to"}, method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Paging> getResourceByResourceType(@RequestParam(value = "from") int from , @RequestParam(value = "to") int to) {
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
	    
	    @RequestMapping(value = "/resources/", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<Paging> getResourceByResourceType() {
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
	  
	    @RequestMapping(value = "/resources", method = RequestMethod.POST, headers = "Accept=application/json")  
	    public ResponseEntity<Resource> addResource(@RequestBody Resource resource) {
			resourceService.addResource(resource);
			return new ResponseEntity<>(resource, HttpStatus.CREATED);
	    }  
	  
	    @RequestMapping(value = "/resources", method = RequestMethod.PUT, headers = "Accept=application/json")  
	    public ResponseEntity<Resource> updateResource(@RequestBody Resource resource) {
	    	resource.setModificationDate(new Date());
	    	Resource resourceFinal;
			resourceFinal = resourceService.updateResource(resource);
	    	return new ResponseEntity<>(resourceFinal, HttpStatus.NO_CONTENT);
	    }  
	  
	    @RequestMapping(value = "/resources/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")  
	    public void deleteResources(@PathVariable("id") String id) {  
	        resourceService.deleteResource(id);  
	    }   
	    
	  
  
}
