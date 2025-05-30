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

import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.dao.SchemaDao;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import gr.uoa.di.madgik.registry.domain.UrlResolver;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.index.DefaultIndexMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by antleb on 7/14/16.
 */
@Service("resourceTypeService")
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        readOnly = true)
public class ResourceTypeServiceImpl implements ResourceTypeService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeServiceImpl.class);

    private final ResourceTypeDao resourceTypeDao;
    private final SchemaDao schemaDao;


    public ResourceTypeServiceImpl(ResourceTypeDao resourceTypeDao, SchemaDao schemaDao) {
        this.resourceTypeDao = resourceTypeDao;
        this.schemaDao = schemaDao;
    }

    private static int isValidUrl(String Url, boolean isFromUrl) {
        URI u;
        try {
            u = new URI(Url);
        } catch (URISyntaxException e) {
            return 0;
        }

        if (u.isAbsolute()) {
            return 1;
        } else {
            if (isFromUrl) {
                return 2;
            } else {
                return 0;
            }
        }
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
    public List<ResourceType> getAllResourceTypeByAlias(String alias) {
        return resourceTypeDao.getAllResourceTypeByAlias(alias);
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
    @Transactional
    public void deleteResourceType(String name) {
        resourceTypeDao.deleteResourceType(name);
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
    @Transactional
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


        if (resourceType.getSchemaUrl() == null || "not_set".equals(resourceType.getSchemaUrl())) {
            resourceType.setSchemaUrl("not_set");
        } else {
            try {
                String schemaStr = UrlResolver.getText(resourceType.getSchemaUrl());
                resourceType.setSchema(schemaStr);
                ArrayList<String> recursionPaths = new ArrayList<>();
                validate(resourceType.getSchemaUrl());
                exportIncludes(resourceType, resourceType.getSchemaUrl(), recursionPaths);
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

    private void exportIncludes(ResourceType resourceType, String baseUrl, ArrayList<String> recursionPaths) throws ServiceException {
        String type = resourceType.getPayloadType();
        boolean isFromUrl;

        if (resourceType.getSchemaUrl().equals("not_set")) {
            isFromUrl = false;
        } else {
            isFromUrl = true;
        }

        if (type.equals("xml")) {
            try {
                validate(resourceType.getSchema());
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                dbFactory.setNamespaceAware(true);
                DocumentBuilder dBuilder;

                dBuilder = dbFactory.newDocumentBuilder();

                Document doc = dBuilder.parse(new InputSource(new StringReader(resourceType.getSchema())));
                doc.getDocumentElement().normalize();


                XPathFactory factory = XPathFactory.newInstance();
                XPath xpath = factory.newXPath();
                final String prefixFinal = "";

                // there's no default implementation for NamespaceContext...seems kind of silly, no?
                xpath.setNamespaceContext(new NamespaceContext() {
                    public String getNamespaceURI(String prefix) {
                        if (prefix == null) return "http://www.w3.org/2001/XMLSchema";
                        else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
                        else if ("xs".equals(prefix)) return "http://www.w3.org/2001/XMLSchema";
                        else if ("xsd".equals(prefix)) return "http://www.w3.org/2001/XMLSchema";
                        return XMLConstants.NULL_NS_URI;
                    }

                    // This method isn't necessary for XPath processing.
                    public String getPrefix(String uri) {
                        throw new UnsupportedOperationException();
                    }

                    // This method isn't necessary for XPath processing either.
                    public Iterator getPrefixes(String uri) {
                        throw new UnsupportedOperationException();
                    }
                });
                String expression = "//xs:include/attribute::schemaLocation|//xs:import/attribute::schemaLocation";
                NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String schemaUrl = nodeList.item(i).getTextContent();

                    logger.debug("Checking schema: " + schemaUrl);

                    int validation = isValidUrl(schemaUrl, isFromUrl);
                    if (validation != 0) {
                        String schemaContent;

                        if (validation == 2) {
                            schemaUrl = baseUrl.replace(baseUrl.substring(baseUrl.lastIndexOf("/") + 1), schemaUrl);
                        }

                        logger.debug("Schema " + schemaUrl + " is already in the db. Ignoring...");
                        nodeList.item(i).setNodeValue(schemaUrl);
                    } else {
                        throw new ServiceException("includes contain relative paths that cannot be resolved");
                    }
                }
                resourceType.setSchema(documentToString(doc));
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }
    }

    private String documentToString(Document document) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(document), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }
}
