/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.dao.IndexedFieldDao;
import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.index.IndexMapper;
import gr.uoa.di.madgik.registry.index.IndexMapperFactory;
import gr.uoa.di.madgik.registry.validation.ResourceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceImpl.class);

    private final ResourceDao resourceDao;
    private final ResourceTypeDao resourceTypeDao;
    private final IndexMapperFactory indexMapperFactory;
    private final IndexedFieldDao indexedFieldDao;
    private final ResourceValidator resourceValidator;

    public ResourceServiceImpl(ResourceDao resourceDao, ResourceTypeDao resourceTypeDao,
                               IndexMapperFactory indexMapperFactory, IndexedFieldDao indexedFieldDao,
                               ResourceValidator resourceValidator) {
        this.resourceDao = resourceDao;
        this.resourceTypeDao = resourceTypeDao;
        this.indexMapperFactory = indexMapperFactory;
        this.indexedFieldDao = indexedFieldDao;
        this.resourceValidator = resourceValidator;
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
    public Long getTotal(ResourceType resourceType) {
        return resourceDao.getTotal(resourceType);
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
    @Transactional(readOnly = true)
    public List<Resource> getResource() {
        return resourceDao.getResource();
    }

    @Override
    @Transactional
    public Resource addResource(Resource resource) throws ServiceException {
        if (resource.getResourceTypeName() != null && resource.getResourceType() == null) {
            resource.setResourceType(resourceTypeDao.getResourceType(resource.getResourceTypeName()));
        }
        if (resource.getResourceType() == null) {
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
                throw new ServiceException("Error saving resource", e);
            }
        }

        return resource;
    }

    @Override
    @Transactional
    public Resource updateResource(Resource resource) throws ServiceException {

        if (resource.getResourceTypeName() != null && resource.getResourceType() == null) {
            resource.setResourceType(resourceTypeDao.getResourceType(resource.getResourceTypeName()));
        }
        if (resource.getResourceType() == null) {
            throw new ServiceException("Resource type does not exist");
        }

        if (resource.getId() == null || resource.getId().isEmpty())
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
    @Transactional
    public Resource changeResourceType(Resource resource, ResourceType resourceType) {
        if (resource.getResourceType() == null && (resource.getResourceTypeName() == null || resource.getResourceTypeName().isEmpty()))
            throw new ServiceException("Resource type not present");
        ResourceType oldResourceType = null;
        if (resource.getResourceType() != null)
            oldResourceType = resource.getResourceType();
        else
            oldResourceType = resourceTypeDao.getResourceType(resource.getResourceTypeName());

        if (oldResourceType == null)
            throw new ServiceException("Resource type not found");

        resource.setResourceType(resourceType);

        Boolean response = checkValid(resource);
        if (!response)
            throw new ServiceException("Failed to validate resource with the new resource type");

        resource.setVersion(generateVersion());
        try {
            resource.setIndexedFields(getIndexedFields(resource));

            for (IndexedField indexedField : resource.getIndexedFields())
                indexedField.setResource(resource);

            resource.setResourceType(resourceType);
            // Save resource using DAO in order to keep the ID of the Resource
            resource = resourceDao.updateResource(resource);
        } catch (Exception e) {
            throw new ServiceException("Error saving resource", e);
        }

        return resource;
    }

    @Override
    @Transactional
    public void deleteResource(String id) {
        resourceDao.deleteResource(resourceDao.getResource(id));
    }

    private List<IndexedField> getIndexedFields(Resource resource) throws ServiceException {

        ResourceType resourceType = resourceTypeDao.getResourceType(resource.getResourceType().getName());
        IndexMapper indexMapper = null;
        try {
            indexMapper = indexMapperFactory.createIndexMapper(resourceType);
            return indexMapper.getValues(resource.getPayload(), resourceType);
        } catch (Exception e) {
            throw new ServiceException("Error extracting fields", e);
        }

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

    private String generateVersion() {
        DateFormat df = new SimpleDateFormat("MMddyyyyHHmmss");
        return df.format(Calendar.getInstance().getTime());
    }
}

