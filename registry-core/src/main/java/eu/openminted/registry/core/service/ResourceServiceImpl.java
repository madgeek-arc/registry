package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.IndexedFieldDao;
import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.dao.VersionDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.index.IndexMapper;
import eu.openminted.registry.core.index.IndexMapperFactory;
import eu.openminted.registry.core.validation.ResourceValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service("resourceService")
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private static Logger logger = LogManager.getLogger(ResourceServiceImpl.class);
    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private VersionDao versionDao;

    @Autowired
    private ResourceTypeDao resourceTypeDao;
    @Autowired
    private IndexMapperFactory indexMapperFactory;
    @Autowired
    private ResourceValidator resourceValidator;
    @Autowired
    private IndexedFieldDao indexedFieldDao;

    public ResourceServiceImpl() {

    }

    @Override
    public Resource getResource(String id) {
        return resourceDao.getResource(id);
    }

    @Override
    public List<Resource> getResource(ResourceType resourceType) {
        return resourceDao.getResource(resourceType);
    }

    @Override
    @Transactional(readOnly = true)
    public void getResourceStream(Consumer<Resource> consumer) {
        resourceDao.getResourceStream().forEach(consumer);

    }

    @Override
    public List<Resource> getResource(ResourceType resourceType, int from, int to) {
        return resourceDao.getResource(resourceType, from, to);
    }

    @Override
    public List<Resource> getResource(int from, int to) {
        return resourceDao.getResource(from, to);
    }

    @Override
    public List<Resource> getResource() {
        return resourceDao.getResource();
    }

    @Override
    public Resource addResource(Resource resource) throws ServiceException {


        if(resource.getResourceTypeName() != null && resource.getResourceType() == null) {
            resource.setResourceType(resourceTypeDao.getResourceType(resource.getResourceTypeName()));
        }
        if(resource.getResourceType() == null) {
            throw new ServiceException("Resource type does not exist");
        }
        if (resource.getPayloadUrl() != null ^ resource.getPayload() != null) {
            resource.setCreationDate(new Date());
            resource.setModificationDate(new Date());
            resource.setPayloadFormat(resource.getResourceType().getPayloadType());
        } else {
            throw new ServiceException("Payload and PayloadUrl conflict : neither set or both set");
        }
        Boolean response = checkValid(resource);
        if (response) {
            resource.setId(UUID.randomUUID().toString());
            resource.setVersion(generateVersion());
            try {
                resource.setIndexedFields(getIndexedFields(resource));

                for (IndexedField indexedField : resource.getIndexedFields())
                    indexedField.setResource(resource);

                resourceDao.addResource(resource);
            } catch (Exception e) {
                logger.error("Error saving resource", e);
                throw new ServiceException(e);
            }
        }

        return resource;
    }

    @Override
    public Resource updateResource(Resource resource) throws ServiceException {

        if(resource.getResourceTypeName() != null  && resource.getResourceType() == null) {
            resource.setResourceType(resourceTypeDao.getResourceType(resource.getResourceTypeName()));
        }
        if(resource.getResourceType() == null) {
            throw new ServiceException("Resource type does not exist");
        }

        if(resource.getId()==null || resource.getId().isEmpty())
            throw new ServiceException("Resource ID cannot be empty");

        Resource oldResource = resourceDao.getResource(resource.getId());
        indexedFieldDao.deleteAllIndexedFields(oldResource);
        resource.setIndexedFields(getIndexedFields(resource));
        for (IndexedField indexedField : resource.getIndexedFields()) {
            indexedField.setResource(resource);
        }
        Boolean response = checkValid(resource);
        if (response) {
            resource.setVersion(generateVersion());
            resourceDao.updateResource(resource);
        }

        return resource;
    }

    @Override
    public Resource changeResourceType(Resource resource, ResourceType resourceType) {
        if(resource.getResourceType() == null)
            throw new ServiceException("Resource type not present");
        ResourceType oldResourceType = resource.getResourceType();
        resource.setResourceType(resourceType);

        Boolean response = checkValid(resource);
        if(!response)
            throw new ServiceException("Failed to validate resource with the new resource type");

        deleteResource(resource.getId());
        resource.setVersion(generateVersion());
        try {
            resource.setIndexedFields(getIndexedFields(resource));

            for (IndexedField indexedField : resource.getIndexedFields())
                indexedField.setResource(resource);

            resourceDao.addResource(resource);
            versionDao.updateParent(resource,resource);
            versionDao.updateParent(oldResourceType,resourceType);
        } catch (Exception e) {
            logger.error("Error saving resource", e);
            throw new ServiceException(e);
        }

        return resource;
    }

    @Override
    public void deleteResource(String id) {
        resourceDao.deleteResource(resourceDao.getResource(id));
    }
    private List<IndexedField> getIndexedFields(Resource resource) throws ServiceException{

        ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType().getName());
        IndexMapper indexMapper = null;
        try {
            indexMapper = indexMapperFactory.createIndexMapper(resourceType);
            return indexMapper.getValues(resource.getPayload(), resourceType);
        } catch (Exception e) {
            logger.error("Error extracting fields", e);
            throw new ServiceException(e);
        }

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
        ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType().getName());

        if (resourceType != null) {
            if (resourceType.getPayloadType().equals(resource.getPayloadFormat())) {
                if (resourceType.getPayloadType().equals("xml")) {
                    //validate xml
                    Boolean output = resourceValidator.validateXML(resource);
                    if (output) {
                        resource.setPayload(resource.getPayload());
                    } else {
                        throw new ServiceException("XML and XSD mismatch");
                    }
                } else if (resourceType.getPayloadType().equals("json")) {

                    Boolean output = resourceValidator.validateJSON(resource);

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

    private String generateVersion(){
        DateFormat df = new SimpleDateFormat("MMddyyyyHHmmss");
        return df.format(Calendar.getInstance().getTime());
    }
}

