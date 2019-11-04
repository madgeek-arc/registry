package eu.openminted.registry.core.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.validation.SchemaInput;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.SAXException;
import org.xmlunit.builder.Input;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Repository("schemaDao")
public class SchemaDaoImpl extends AbstractDao<Schema> implements SchemaDao {

    private static Logger logger = LogManager.getLogger(SchemaDaoImpl.class);

    private LoadingCache<String, javax.xml.validation.Schema> schemaXMLLoader;

    private LoadingCache<String, org.everit.json.schema.Schema> schemaJSONLoader;

    private static final String XSD_SCHEMA = XMLConstants.W3C_XML_SCHEMA_NS_URI + ".xsd";

    public SchemaDaoImpl() {
        super();
        CacheLoader<String, javax.xml.validation.Schema> xmlLoader;
        xmlLoader = new XMLSchemaLoader(this);
        CacheLoader<String, org.everit.json.schema.Schema> jsonLoader;
        jsonLoader = new JSONSchemaLoader(this);
        schemaXMLLoader = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(xmlLoader);
        schemaJSONLoader = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(jsonLoader);
    }

    @Override
    public Schema getSchema(String id) {
        return getSingleResult("id",id);
    }

    @Override
    public Schema getSchemaByUrl(String originalURL) {
        return getSingleResult("originalUrl",originalURL);
    }

    @Override
    public void addSchema(Schema schema) {
        if (schema.getId() == null) {
            schema.setId(stringToMd5(schema.getSchema() + schema.getOriginalUrl()));
        }
        persist(schema);
        logger.info("Added schema with url = " + schema.getOriginalUrl());
    }

    @Override
    public void deleteSchema(Schema schema) {
        delete(schema);
    }

    @Override
    public javax.xml.validation.Schema loadXMLSchema(ResourceType resourceType) {
        return schemaXMLLoader.getUnchecked(resourceType.getName());
    }

    @Override
    public org.everit.json.schema.Schema loadJSONSchema(ResourceType resourceType) {
        return this.schemaJSONLoader.getUnchecked(resourceType.getName());
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId, String baseURI) {
        try {
            logger.info("Processing schema with systemId " + systemId);
            eu.openminted.registry.core.domain.Schema existing;
            if(baseURI==null)
                existing = getSchemaByUrl(systemId);
            else
                existing = getSchemaByUrl(replaceLastSegment(baseURI, systemId));

            if (existing != null)
                return new SchemaInput(publicId, systemId, IOUtils.toInputStream(existing.getSchema()), baseURI);

            URL schemaURL = new URL("");
            if(baseURI!=null) {
                schemaURL = new URL(new URL(baseURI), systemId);
            } else {
                schemaURL = new URL(systemId);
            }
            String schemaStr = IOUtils.toString(schemaURL.openStream());
            if (validateXML(schemaStr)) {
                String md5 = stringToMd5(schemaStr + systemId);
                if (getSchema(md5) == null) {
                    Schema schema = new Schema();
                    schema.setSchema(schemaStr);
                    if(baseURI!=null)
                        schema.setOriginalUrl(replaceLastSegment(baseURI,systemId));
                    else
                        schema.setOriginalUrl(systemId);
                    schema.setId(md5);
                    addSchema(schema);
                }
            }
            return new SchemaInput(publicId, systemId, IOUtils.toInputStream(schemaStr), baseURI);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    public String replaceLastSegment(String url, String replacingPath){
        return url.replace(url.substring(url.lastIndexOf('/')+1), replacingPath);
    }

    private boolean validateXML(String schema) throws IOException {

        Validator validator = schemaXMLLoader
                .getUnchecked(XSD_SCHEMA)
                .newValidator();
        try {
            validator.validate(new StreamSource(IOUtils.toInputStream(schema)));
        } catch (SAXException e) {
            return false;
        }
        return true;
    }

    private static String stringToMd5(String schema) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(schema.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @Override
    public InputStream get(String schemaUrl) {
        logger.info("Loading " + schemaUrl);
        Schema existing = getSchemaByUrl(schemaUrl);
        if (existing != null) {
            return IOUtils.toInputStream(existing.getSchema());
        } else {
            URL url;
            String jsonSchema;
            try {
                url = new URL(schemaUrl);
                jsonSchema = IOUtils.toString(url.openConnection().getInputStream());
                String md5 = stringToMd5(jsonSchema);
                if (getSchema(md5) == null) {
                    Schema schema = new Schema();
                    schema.setOriginalUrl(schemaUrl);
                    schema.setSchema(jsonSchema);
                    schema.setId(md5);
                    addSchema(schema);
                }

            } catch (Exception e) {
                throw new ServiceException("JSON schema URL is not valid " + schemaUrl, e);
            }
            return IOUtils.toInputStream(jsonSchema);
        }
    }

    private static class XMLSchemaLoader extends CacheLoader<String, javax.xml.validation.Schema> {

        private SchemaFactory factory;

        private SchemaFactory xsdFactory;

        private SchemaDao schemaDao;

        XMLSchemaLoader(SchemaDao schemaDao) {
            this.schemaDao = schemaDao;
            factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            xsdFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(schemaDao);
        }

        @Override
        public javax.xml.validation.Schema load(String name) throws Exception {
            if (name.equals(XSD_SCHEMA)) {
                return xsdFactory.newSchema(Input.fromURI(name).build());
            } else {
                InputStream streamXsd = IOUtils.toInputStream(schemaDao.getSchemaByUrl(name).getSchema());
                Source schemaFile = new StreamSource(streamXsd);
                return factory.newSchema(schemaFile);
            }
        }
    }

    private static class JSONSchemaLoader extends CacheLoader<String, org.everit.json.schema.Schema> {

        private SchemaDao schemaDao;

        JSONSchemaLoader(SchemaDao schemaDao) {
            this.schemaDao = schemaDao;
        }

        @Override
        public org.everit.json.schema.Schema load(String name) throws Exception {
            Schema schema = schemaDao.getSchemaByUrl(name);
            String schemaString = schema.getSchema();
            return SchemaLoader.load(new JSONObject(schemaString), schemaDao);
        }
    }
}
