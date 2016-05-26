package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.DaoException;
import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.index.IndexMapper;
import eu.openminted.registry.core.index.IndexMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

	public String addResource(Resource resource) {

		resource.setIndexedFields(getIndexedFields(resource));

//		try {
//			// dao.skata()..
//		} catch (DaoException de) {
//			throw new ServiceException("Error saving resource", de);
//		}

		return resourceDao.addResource(resource);
	}

	public Resource updateResource(Resource resource) {

		resource.setIndexedFields(getIndexedFields(resource));

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
}
