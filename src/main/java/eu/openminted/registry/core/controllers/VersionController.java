package eu.openminted.registry.core.controllers;

import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.exception.VersionNotFoundException;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Transactional
public class VersionController {

	@Autowired
	VersionService versionService;

	@Autowired
	ResourceTypeService resourceTypeService;

	@Autowired
	ResourceService resourceService;

	@RequestMapping(value = "/version/{resourceType}/", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<List<Version>> getVersionsByResourceType(@PathVariable("resourceType") String resourceType) {

	   	List<Version> versions = versionService.getVersionsByResourceType(resourceType);

	   	if(versions==null || versions.isEmpty()){
			throw new VersionNotFoundException();
	   	}else{
	   		return new ResponseEntity<>(versions, HttpStatus.OK);
	   	}

	}

	@RequestMapping(value = "/version/{resourceType}/{resource}/", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<List<Version>> getVersionsByResource(@PathVariable("resourceType") String resourceType,
															   @PathVariable("resource") String resource) {
		List<Version> versions = versionService.getVersionsByResource(resource);
		versions.stream().filter(v -> v.getVersion()=="1").collect(Collectors.toList());

		if(versions==null || versions.isEmpty()){
			throw new VersionNotFoundException();
		}else{
			return new ResponseEntity<>(versions, HttpStatus.OK);
		}

	}

	@RequestMapping(value = "/version/{resourceType}/{resource}/{version}/", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<Version> getVersion(@PathVariable("resourceType") String resourceType,
											  @PathVariable("resource") String resource,
											  @PathVariable("version") String versionNumber) {

		Version version = versionService.getVersion(resource,versionNumber);

		if(version==null){
			throw new VersionNotFoundException();
		}else{
			return new ResponseEntity<>(version, HttpStatus.OK);
		}

	}

	@RequestMapping(value = "/version/", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<List<Version>> getVersions() {

		List<Version> versions = versionService.getAllVersions();

		if(versions==null || versions.isEmpty()){
			throw new VersionNotFoundException();
		}else{
			return new ResponseEntity<>(versions, HttpStatus.OK);
		}

	}

}
