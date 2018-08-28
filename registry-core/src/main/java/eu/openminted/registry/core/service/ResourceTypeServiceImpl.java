package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.dao.SchemaDao;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.domain.UrlResolver;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.index.DefaultIndexMapper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by antleb on 7/14/16.
 */
@Service("resourceTypeService")
@Transactional
public class ResourceTypeServiceImpl implements ResourceTypeService {

    private static Logger logger = LogManager.getLogger(ResourceTypeService.class);

    @Autowired
    ResourceTypeDao resourceTypeDao;

    @Autowired
    ResourceService resourceService;

    @Autowired
    SchemaDao schemaDao;

    @Value("${registry.host}")
    private String baseUrl;

    public ResourceTypeServiceImpl() {

    }

    @Override
    public Schema getSchema(String id) {
        return schemaDao.getSchema(id);
    }

    @Override
    public ResourceType getResourceType(String name) {
        return resourceTypeDao.getResourceType(name);
    }

    @Override
    public List<ResourceType> getAllResourceType() {

        return resourceTypeDao.getAllResourceType();
    }

    @Override
    public List<ResourceType> getAllResourceType(int from, int to) {
        return resourceTypeDao.getAllResourceType(from, to);
    }

    @Override
    public Set<IndexField> getResourceTypeIndexFields(String name) {
        return resourceTypeDao.getResourceTypeIndexFields(name);
    }

    @Override
    public void deleteResourceType(String name) {
        resourceTypeDao.deleteResourceType(resourceTypeDao.getResourceType(name));
    }

    private boolean validate(String schema) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(schemaDao);

        try {
            schemaFactory.newSchema(new URL(schema));

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public ResourceType addResourceType(ResourceType resourceType) throws ServiceException {
        Schema schema = new Schema();

        ResourceType existing = resourceTypeDao.getResourceType(resourceType.getName());
        if (existing != null) {
            return existing;
        }

        if (resourceType.getSchemaUrl() == null && resourceType.getSchema() == null) {
            throw new ServiceException("Neither SchemaUrl nor Schema have been set");
        } else if (resourceType.getSchemaUrl() != null && resourceType.getSchema() != null) {
            throw new ServiceException("Both Schema and SchemaUrl are set");
        }



        if (resourceType.getSchemaUrl() == null) {
            resourceType.setSchemaUrl("not_set");
        } else {
            try {
                resourceType.setSchema(UrlResolver.getText(resourceType.getSchemaUrl()));
                validate(resourceType.getSchemaUrl());
            } catch (Exception e) {
                throw new ServiceException(e.getMessage());
            }
        }



        if (resourceType.getIndexMapperClass() == null)
            resourceType.setIndexMapperClass(DefaultIndexMapper.class.getName());

        if (resourceType.getIndexFields() != null) {
            for (IndexField field : resourceType.getIndexFields())
                field.setResourceType(resourceType);
        }

        try {
            resourceTypeDao.addResourceType(resourceType);
        } catch (Exception e) {
            throw new ServiceException(e);
        }

        if (resourceType.getSchemaUrl() != null) {
            schema.setOriginalUrl(resourceType.getSchemaUrl());
        }
        Schema resourceTypeSchema = new Schema();
        resourceTypeSchema.setSchema(resourceType.getSchema());
        resourceTypeSchema.setOriginalUrl(resourceType.getName());
        schemaDao.addSchema(resourceTypeSchema);

        return resourceType;
    }

    public ResourceTypeDao getResourceTypeDao() {
        return resourceTypeDao;
    }

    public void setResourceTypeDao(ResourceTypeDao resourceTypeDao) {
        this.resourceTypeDao = resourceTypeDao;
    }


    protected String getBaseEnvLinkURL() {
        return baseUrl;
    }
}