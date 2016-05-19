package controllers;

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

import classes.Paging;
import classes.ResourceType;
import classes.Tools;
import service.ResourceTypeService;



@RestController
public class ResourceTypeController {

	   @Autowired
	   ResourceTypeService resourceTypeService;  
	  
	    @RequestMapping(value = "/resourceType/{name}", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceTypeByName(@PathVariable("name") String name) { 
	    	ResponseEntity<String> responseEntity;
	    	ResourceType resourceType = resourceTypeService.getResourceType(name);
	    	if(resourceType==null){
	    		responseEntity = new ResponseEntity<String>(HttpStatus.NOT_FOUND);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(resourceType), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    } 
	    
	    @RequestMapping(value = "/resourceType/",params = {"from"} ,method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceTypes(@RequestParam(value = "from") int from) {  
	    	List<ResourceType> results = resourceTypeService.getAllResourceType(from,0);
	    	Paging paging = new Paging(results.size(), 0, results.size()-1, results);
	    	ResponseEntity<String> responseEntity;
	    	if(results.size()==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;	   
	    	
	    }
	    
	    @RequestMapping(value = "/resourceType/",params = {"from","to"} ,method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceTypes(@RequestParam(value = "from") int from , @RequestParam(value = "from") int to) {  
	    	List<ResourceType> results = resourceTypeService.getAllResourceType(from,to);
	    	int total = resourceTypeService.getAllResourceType().size();
	    	Paging paging = new Paging(total, from, to, results);
	    	ResponseEntity<String> responseEntity;
	    	if(total==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    } 
	    
	    @RequestMapping(value = "/resourceType/", method = RequestMethod.GET, headers = "Accept=application/json")  
	    public ResponseEntity<String> getResourceTypes() {  
	    	List<ResourceType> results = resourceTypeService.getAllResourceType();
	    	Paging paging = new Paging(results.size(), 0, results.size()-1, results);
	    	ResponseEntity<String> responseEntity;
	    	if(results.size()==0){
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.NO_CONTENT);
	    	}else{
	    		responseEntity = new ResponseEntity<String>(Tools.objToJson(paging), HttpStatus.OK);
	    	}
	    	return responseEntity;
	    } 
	  
	    @RequestMapping(value = "/resourceType", method = RequestMethod.POST, headers = "Accept=application/json")  
	    public ResponseEntity<String> addResourceType(@RequestBody ResourceType resourceType) { 
	    	ResponseEntity<String> responseEntity;
	    	if(resourceType.getSchemaUrl()==null && resourceType.getSchema()==null){
	    		responseEntity = new ResponseEntity<String>("{\"error\":\"Neither SchemaUrl nor Schema have been set.\"}", HttpStatus.INTERNAL_SERVER_ERROR);
	    	}else if(resourceType.getSchemaUrl()!=null && resourceType.getSchema()!=null){
	    		responseEntity = new ResponseEntity<String>("{\"error\":\"Both Schema and SchemaUrl are set\"}", HttpStatus.INTERNAL_SERVER_ERROR);
	    	}else{
	    		if(resourceType.getSchemaUrl()==null){
	    			resourceType.setSchemaUrl("not_set");
	    		}else{
	    			try {
	    					String output = "";
	    					output = Tools.getText(resourceType.getSchemaUrl());
	   						resourceType.setSchema(output);
					} catch (Exception e) {
						e.printStackTrace();
					}
	    		}
	    	}
	    	resourceType.setCreationDate(new Date());
	    	resourceType.setModificationDate(new Date());
	    	responseEntity = new ResponseEntity<String>(Tools.objToJson(resourceType), HttpStatus.CREATED);
	    	resourceTypeService.addResourceType(resourceType);
	    	return responseEntity;
	    }  
  
}
