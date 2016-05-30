package eu.openminted.registry.core.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Tools;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.index.IndexMapper;
import eu.openminted.registry.core.index.IndexMapperFactory;

@Service("resourceService")
@Transactional
public class ResourceService {

	@Autowired
	private ResourceDao resourceDao;
	@Autowired
	private ResourceTypeDao resourceTypeDao;
	@Autowired
	private IndexMapperFactory indexMapperFactory;

	public ResourceService() {

	}

	public Resource getResource(String resourceType, String id) {
		Resource resource = resourceDao.getResource(resourceType, id);
		return resource;
	}

	public List<Resource> getResource(String resourceType) {
		List<Resource> resources = resourceDao.getResource(resourceType);
		return resources;
	}

	public List<Resource> getResource(String resourceType, int from, int to) {
		List<Resource> resources = resourceDao.getResource(resourceType, from, to);
		return resources;
	}

	public List<Resource> getResource(int from, int to) {
		List<Resource> resources = resourceDao.getResource(from, to);
		return resources;
	}

	public List<Resource> getResource() {
		List<Resource> resources = resourceDao.getResource();
		return resources;
	}

	public void addResource(Resource resource) throws ServiceException {

		resource.setIndexedFields(getIndexedFields(resource));

		if (resource.getIndexedFields() != null)
			for (IndexedField indexedField:resource.getIndexedFields())
				indexedField.setResource(resource);
			
			String response = checkValid(resource);
			if(response.equals("OK")){
				resource.setId(UUID.randomUUID().toString());
				resourceDao.addResource(resource);
			}else{
				throw new ServiceException(response);
			}
			
	}

	public Resource updateResource(Resource resource){

		resource.setIndexedFields(getIndexedFields(resource));

		if (resource.getIndexedFields() != null)
			for (IndexedField indexedField:resource.getIndexedFields())
				indexedField.setResource(resource);

		resourceDao.updateResource(resource);

		return resource;
	}

	public void deleteResource(String id) {
		resourceDao.deleteResource(id);
	}

	private List<IndexedField> getIndexedFields(Resource resource) {

		ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType());
		IndexMapper indexMapper = null;

		try {
			indexMapper = indexMapperFactory.createIndexMapper(resourceType);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return indexMapper.getValues(resource.getPayload(), resourceType);
	}

	public ResourceDao getResourceDao() {
		return resourceDao;
	}

	public void setResourceDao(ResourceDao resourceDao) {
		this.resourceDao = resourceDao;
	}

	public ResourceTypeDao getResourceTypeDao() {
		return resourceTypeDao;
	}

	public void setResourceTypeDao(ResourceTypeDao resourceTypeDao) {
		this.resourceTypeDao = resourceTypeDao;
	}
	
	private String checkValid(Resource resource){
		String response = "";
		ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType());

		if (resourceType != null) {
			if (resourceType.getPayloadType().equals(resource.getPayloadFormat())) {
				if (resourceType.getPayloadType().equals("xml")) {
					//validate xml
					String output = Tools.validateXMLSchema(resourceType.getSchema(), resource.getPayload());
					if (output.equals("true")) {
						resource.setPayload(resource.getPayload());
						response = "OK";
					} else {
						response = "XML and XSD mismatch";
					}
				} else if (resourceType.getPayloadType().equals("json")) {

					//validate json
//					String jsonResponse = Tools.validateJSONSchema(resourceType.getSchema(), resource.getPayload());
					String jsonResponse = "true";

					if (jsonResponse.equals("true")) {
						resource.setPayload(resource.getPayload());
						response = "OK";
					} else {
						response = "JSON and Schema missmatch";
					}
				} else {
					//payload type not supported
					response = "type not supported";
				}
			} else {
				//payload and schema format do not match, we cant validate
				response = "payload and schema format are different";
			}
		} else {
			//resource type not found
			response = "resource type not found";
		}
		
		return response;
	}
}
