package eu.openminted.registry.core.controllers;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.Tools;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ServiceException;

@RestController
public class ResourceController {

	   @Autowired
	   ResourceService resourceService;
	  
	    @RequestMapping(value = "/resources/{resourceType}/{id}", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceById(@PathVariable("resourceType") String resourceType,@PathVariable("id") String id) {  
	    	Resource resource = resourceService.getResource(resourceType,id);
	    	ResponseEntity<String> responseEntity;
	    	if(resource==null){
	    		responseEntity = new ResponseEntity<String>(HttpStatus.NOT_FOUND);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(resource), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceByResourceType(@PathVariable("resourceType") String resourceType) {  
	        List<Resource> results = resourceService.getResource(resourceType);
	    	Paging paging = new Paging(results.size(), 0, results.size()-1, results);
	    	ResponseEntity<String> responseEntity;
	    	if(results.size()==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    } 
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"from"},method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceByResourceType(@PathVariable("resourceType") String resourceType ,@RequestParam(value = "from") int from) {  
	    	List<Resource> results = resourceService.getResource(resourceType,from,0);
	    	int total = resourceService.getResource(resourceType).size();
	    	Paging paging = new Paging(total, from, total-1, results);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    }
	    
	    @RequestMapping(value = "/resources/{resourceType}", params = {"from","to"}, method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceByResourceType(@PathVariable("resourceType") String resourceType ,@RequestParam(value = "from") int from , @RequestParam(value = "to") int to ) {  
	    	List<Resource> results = resourceService.getResource(resourceType,from,to);
	    	int total = resourceService.getResource(resourceType).size();
	    	Paging paging = new Paging(total, from, to, results);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    } 
	    
	    @RequestMapping(value = "/resources/",params = {"from"}, method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceByResourceType(@RequestParam(value = "from") int from ) {  
	    	List<Resource> results = resourceService.getResource(from,0);
	    	int total = resourceService.getResource().size();
	    	Paging paging = new Paging(total, from, total-1, results);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    }
	    
	    @RequestMapping(value = "/resources/",params = {"from","to"}, method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceByResourceType(@RequestParam(value = "from") int from , @RequestParam(value = "to") int to) {  
	    	List<Resource> results = resourceService.getResource(from,to);
	    	int total = resourceService.getResource().size();
	    	Paging paging = new Paging(total, from, to, results);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    }
	    
	    @RequestMapping(value = "/resources/", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceByResourceType() {  
	    	List<Resource> results = resourceService.getResource();
	    	int total = resourceService.getResource().size();
	    	Paging paging = new Paging(total, 0, total-1, results);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    } 
	  
	    @RequestMapping(value = "/resources", method = RequestMethod.POST, headers = "Accept=application/json")  
	    public ResponseEntity<String> addResource(@RequestBody Resource resource) {  
	    	ResponseEntity<String> responseEntity;
	    	
	    	if(resource.getPayloadUrl()==null && resource.getPayload()==null){
	    		responseEntity = new ResponseEntity<String>("{\"error\":\"Neither PayloadUrl nor Payload have been set.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
	    	}else if(resource.getPayloadUrl()!=null && resource.getPayload()!=null){
	    		responseEntity = new ResponseEntity<String>("{\"error\":\"Both Payload and PayloadUrl are set\"}", HttpStatus.INTERNAL_SERVER_ERROR);
	    	}else{
	    		if(resource.getPayloadUrl()==null){
	    			resource.setPayloadUrl("not_set");
	    		}else{
	    			try {
						resource.setPayload(Tools.getText(resource.getPayloadUrl()));
					} catch (Exception e) {
						e.printStackTrace();
					}
	    		}

				resource.setCreationDate(new Date());
				resource.setModificationDate(new Date());
				
				responseEntity = new ResponseEntity<String>(Tools.objToJson(resource), HttpStatus.CREATED);
				try{
					resourceService.addResource(resource);
				}catch(ServiceException ex){
					responseEntity = new ResponseEntity<String>("{\"error\":\""+ex.getMessage()+"\"}", HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}

	        return responseEntity;  
	    }  
	  
	    @RequestMapping(value = "/resources", method = RequestMethod.PUT, headers = "Accept=application/json")  
	    public ResponseEntity<String> updateResource(@RequestBody Resource resource) {
	    	resource.setModificationDate(new Date());
	    	Resource resourceFinal = resourceService.updateResource(resource);
	    	ResponseEntity<String> responseEntity;
	    	if(resourceFinal==null){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(resourceFinal), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(resourceFinal), HttpStatus.OK);
	    	}
	        return   responseEntity;
	    }  
	  
	    @RequestMapping(value = "/resources/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")  
	    public void deleteResources(@PathVariable("id") String id) {  
	        resourceService.deleteResource(id);  
	    }   
	    
	  
  
}
