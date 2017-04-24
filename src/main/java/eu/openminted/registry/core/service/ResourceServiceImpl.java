	package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Tools;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.index.IndexMapper;
import eu.openminted.registry.core.index.IndexMapperFactory;
import eu.openminted.registry.core.validation.ResourceValidator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.management.ServiceNotFoundException;

@Service("resourceService")
@Transactional
public class ResourceServiceImpl implements ResourceService {

	private static Logger logger = Logger.getLogger(ResourceServiceImpl.class);
	@Autowired
	private ResourceDao resourceDao;
	@Autowired
	private ResourceTypeDao resourceTypeDao;
	@Autowired
	private IndexMapperFactory indexMapperFactory;
	@Autowired
	private ResourceValidator resourceValidator;

	public ResourceServiceImpl() {

	}


	@Override public Resource getResource(String resourceType, String id) {
		Resource resource = resourceDao.getResource(resourceType, id);
		return resource;
	}

	@Override public List<Resource> getResource(String resourceType) {
		List<Resource> resources = resourceDao.getResource(resourceType);
		return resources;
	}

	@Override public List<Resource> getResource(String resourceType, int from, int to) {
		List<Resource> resources = resourceDao.getResource(resourceType, from, to);
		return resources;
	}

	@Override public List<Resource> getResource(int from, int to) {
		List<Resource> resources = resourceDao.getResource(from, to);
		return resources;
	}

	@Override public List<Resource> getResource() {
		List<Resource> resources = resourceDao.getResource();
		return resources;
	}

	@Override public Resource addResource(Resource resource) throws ServiceException {
//		if(resource.getIndexedFields()!=null)
		
		if(resource.getPayloadUrl()==null && resource.getPayload()==null){
    		throw new ServiceException("{\"error\":\"Neither PayloadUrl nor Payload have been set.\"}");
    	}else if(resource.getPayloadUrl()!=null && resource.getPayload()!=null){
    		throw new ServiceException("{\"error\":\"Both Payload and PayloadUrl are set\"}");
    	}else{
    		if(resource.getPayloadUrl()==null){
    			resource.setPayloadUrl("not_set");
    		}else{
    			try {
					resource.setPayload(Tools.getText(resource.getPayloadUrl()));
				} catch (Exception e) {
					throw new ServiceException("{\"error\":\""+e.getMessage()+"\"}");
				}
    		}

			resource.setCreationDate(new Date());
			resource.setModificationDate(new Date());
			
		}


		Boolean response = checkValid(resource);
		if(response){
			resource.setId(UUID.randomUUID().toString());

			try {
				resource.setIndexedFields(getIndexedFields(resource));

				logger.debug("indexed fields: " + resource.getIndexedFields().size());

				if (resource.getIndexedFields() != null)
					for (IndexedField indexedField:resource.getIndexedFields())
						indexedField.setResource(resource);

				resourceDao.addResource(resource);
			} catch (Exception e) {
				logger.error("Error saving resource", e);
				throw new ServiceException(e);
			}
		}
		
		return resource;
	}

	@Override public Resource updateResource(Resource resource) throws ServiceException{

		resource.setIndexedFields(getIndexedFields(resource));

		if (resource.getIndexedFields() != null)
			for (IndexedField indexedField:resource.getIndexedFields()){
				indexedField.setResource(resource);
			}
		Boolean response = checkValid(resource);
		if(response){
			resourceDao.updateResource(resource);
		}

		return resource;
	}

	@Override public void deleteResource(String id) {
		resourceDao.deleteResource(id);
	}

	private List<IndexedField> getIndexedFields(Resource resource) {

		ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType());
		IndexMapper indexMapper = null;

		try {
			indexMapper = indexMapperFactory.createIndexMapper(resourceType);
		} catch (Exception e) {
			logger.error("Error extracting fields", e);
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

	private Boolean checkValid(Resource resource) {
		ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType());

		if (resourceType != null) {
			if (resourceType.getPayloadType().equals(resource.getPayloadFormat())) {
				if (resourceType.getPayloadType().equals("xml")) {
					//validate xml
					Boolean output = resourceValidator.validateXML(resource.getResourceType(), resource.getPayload());
					if (output) {
						resource.setPayload(resource.getPayload());
					} else {
						throw new ServiceException("XML and XSD mismatch");
					}
				} else if (resourceType.getPayloadType().equals("json")) {

					Boolean output = resourceValidator.validateJSON(resourceType.getSchema(), resource.getPayload());

					if (output) {
						resource.setPayload(resource.getPayload());
					} else {
						throw new ServiceException("JSON and Schema mismatch");
					}
				} else {
					//payload type not supported
					throw new ServiceException("type not supported");
				}
			} else {
				//payload and schema format do not match, we cant validate
				throw new ServiceException("payload and schema format are different");
			}
		} else {
			//resource type not found
			throw new ServiceException("resource type not found");
		}

		return true;
	}
}

