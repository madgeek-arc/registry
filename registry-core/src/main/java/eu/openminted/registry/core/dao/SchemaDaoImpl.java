package eu.openminted.registry.core.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.validation.SchemaInput;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
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

@Repository("schemaDao")
public class SchemaDaoImpl extends AbstractDao<String, Schema> implements SchemaDao {

    private static Logger logger = LogManager.getLogger(SchemaDaoImpl.class);

    private LoadingCache<String, javax.xml.validation.Schema> schemaLoader;

    private static final String XSD_SCHEMA = XMLConstants.W3C_XML_SCHEMA_NS_URI + ".xsd";

    public SchemaDaoImpl() {
        super();
        CacheLoader<String, javax.xml.validation.Schema> loader;
        loader = new CustomSchemaLoader(this);
        schemaLoader = CacheBuilder.newBuilder().build(loader);
    }

    @Override
	public Schema getSchema(String id) {
		// TODO Auto-generated method stub
		Criteria cr = getSession().createCriteria(Schema.class);
		cr.add(Restrictions.eq("id", id));
		if(cr.list().size()==0){
			return null;
		}else{
			return (Schema) cr.list().get(0);
		}
	}
	
	@Override
	public Schema getSchemaByUrl(String originalURL) {
		// TODO Auto-generated method stub
		Criteria cr = getSession().createCriteria(Schema.class);
		cr.add(Restrictions.eq("originalUrl", originalURL));
		if(cr.list().size()==0){
			return null;
		}else{
			return (Schema) cr.list().get(0);
		}
	}

	@Override
	public void addSchema(Schema schema) {
        if(schema.getId() == null) {
            schema.setId(stringToMd5(schema.getSchema() + schema.getOriginalUrl()));
        }
		getSession().saveOrUpdate(schema);
        logger.info("Added schema with url = " + schema.getOriginalUrl());
	}

	@Override
	public void deleteSchema(Schema schema) {
		getSession().delete(schema);
	}

    @Override
    public javax.xml.validation.Schema loadSchema(String url) {
        return this.schemaLoader.getUnchecked(url);
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId, String baseURI) {
        try {
            logger.info("Processing schema with systemId " + systemId);
            eu.openminted.registry.core.domain.Schema existing;
            existing = getSchemaByUrl(systemId);
            if(existing != null)
                return new SchemaInput(publicId,systemId,IOUtils.toInputStream(existing.getSchema()),baseURI);
            URL schemaURL = new URL(new URL(baseURI),systemId);
            String schemaStr = IOUtils.toString(schemaURL.openStream());
            if(validate(schemaStr)){
                String md5 = stringToMd5(schemaStr + systemId);
                if(getSchema(md5)==null) {
                    Schema schema = new Schema();
                    schema.setSchema(schemaStr);
                    schema.setOriginalUrl(systemId);
                    schema.setId(md5);
                    addSchema(schema);
                }
            }
            return new SchemaInput(publicId,systemId,IOUtils.toInputStream(schemaStr),baseURI);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    private boolean validate(String schema) throws IOException {

        Validator validator = schemaLoader
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

    private static class CustomSchemaLoader extends CacheLoader<String, javax.xml.validation.Schema> {

        private SchemaFactory factory;

        private SchemaFactory xsdFactory;

        private SchemaDao schemaDao;

        CustomSchemaLoader(SchemaDao schemaDao) {
            this.schemaDao = schemaDao;
            factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            xsdFactory= SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(schemaDao);
        }

        @Override
        public javax.xml.validation.Schema load(String name) throws Exception {
            if(name.equals(XSD_SCHEMA)) {
                return xsdFactory.newSchema(Input.fromURI(name).build());
            } else {
                InputStream streamXsd = IOUtils.toInputStream(schemaDao.getSchemaByUrl(name).getSchema());
                Source schemaFile = new StreamSource(streamXsd);
                return factory.newSchema(schemaFile);
            }
        }
    }
}
