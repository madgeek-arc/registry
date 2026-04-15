/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.index.IndexMapper;
import gr.uoa.di.madgik.registry.index.IndexMapperFactory;
import gr.uoa.di.madgik.registry.service.AuditActorProvider;
import gr.uoa.di.madgik.registry.validation.ResourceSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.Instant;
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
    private final ResourceSchemaValidator resourceSchemaValidator;
    private final AuditActorProvider auditActorProvider;

    public ResourceServiceImpl(ResourceDao resourceDao, ResourceTypeDao resourceTypeDao,
                               IndexMapperFactory indexMapperFactory,
                               ResourceSchemaValidator resourceSchemaValidator,
                               AuditActorProvider auditActorProvider) {
        this.resourceDao = resourceDao;
        this.resourceTypeDao = resourceTypeDao;
        this.indexMapperFactory = indexMapperFactory;
        this.resourceSchemaValidator = resourceSchemaValidator;
        this.auditActorProvider = auditActorProvider;
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
            resource.setCreationDate(Instant.now());
            resource.setModificationDate(Instant.now());
            String actor = currentActor();
            resource.setCreatedBy(actor);
            resource.setModifiedBy(actor);
            resource.setPayloadFormat(resource.getResourceType().getPayloadType());
        } else {
            throw new ServiceException("Payload and PayloadUrl conflict : neither set or both set");
        }

        resourceSchemaValidator.validate(resource);
        resource.setId(UUID.randomUUID().toString());
        resource.regenerateVersion(); // version monitor aspect depends on this change
        try {
            resource.setIndexedFields(getIndexedFields(resource));

            for (IndexedField indexedField : resource.getIndexedFields())
                indexedField.setResource(resource);

            resourceDao.addResource(resource);
        } catch (Exception e) {
            throw new ServiceException("Error saving resource", e);
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
        if (oldResource == null) {
            throw new ServiceException("Resource not found");
        }

        oldResource.setResourceType(resource.getResourceType());
        oldResource.setPayload(resource.getPayload());
        oldResource.setPayloadFormat(resource.getPayloadFormat());
        oldResource.setPayloadUrl(resource.getPayloadUrl());
        oldResource.setSearchableArea(resource.getSearchableArea());
        oldResource.setResourceTypeName(resource.getResourceTypeName());
        oldResource.setModifiedBy(currentActor());

        List<IndexedField> indexedFields = getIndexedFields(oldResource);
        for (IndexedField indexedField : indexedFields) {
            indexedField.setResource(oldResource);
        }

        if (oldResource.getIndexedFields() == null) {
            oldResource.setIndexedFields(new ArrayList<>());
        } else {
            oldResource.getIndexedFields().clear();
        }
        oldResource.getIndexedFields().addAll(indexedFields);

        resourceSchemaValidator.validate(oldResource);
        oldResource.regenerateVersion(); // version monitor aspect depends on this change
        oldResource = resourceDao.updateResource(oldResource);

        return oldResource;
    }

    @Override
    @Transactional
    public Resource changeResourceType(Resource resource, ResourceType resourceType) {
        if (resource.getResourceType() == null && (resource.getResourceTypeName() == null || resource.getResourceTypeName().isEmpty()))
            throw new ServiceException("Resource type not present");
        Resource managedResource = resource.getId() == null ? null : resourceDao.getResource(resource.getId());
        if (managedResource == null) {
            throw new ServiceException("Resource not found");
        }

        ResourceType oldResourceType = null;
        if (resource.getResourceType() != null)
            oldResourceType = resource.getResourceType();
        else
            oldResourceType = resourceTypeDao.getResourceType(resource.getResourceTypeName());

        if (oldResourceType == null)
            throw new ServiceException("Resource type not found");

        managedResource.setResourceType(resourceType);
        managedResource.setResourceTypeName(resourceType.getName());
        managedResource.setModifiedBy(currentActor());

        resourceSchemaValidator.validate(managedResource);

        managedResource.regenerateVersion(); // version monitor aspect depends on this change
        try {
            List<IndexedField> indexedFields = getIndexedFields(managedResource);

            for (IndexedField indexedField : indexedFields)
                indexedField.setResource(managedResource);

            if (managedResource.getIndexedFields() == null) {
                managedResource.setIndexedFields(new ArrayList<>());
            } else {
                managedResource.getIndexedFields().clear();
            }
            managedResource.getIndexedFields().addAll(indexedFields);

            // Save resource using DAO in order to keep the ID of the Resource
            managedResource = resourceDao.updateResource(managedResource);
        } catch (Exception e) {
            throw new ServiceException("Error saving resource", e);
        }

        return managedResource;
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

    private String currentActor() {
        String actor = auditActorProvider.currentActor();
        return actor == null || actor.isBlank() ? "system" : actor;
    }
}
