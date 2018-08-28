package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Schema;
import org.w3c.dom.ls.LSResourceResolver;

public interface SchemaDao extends LSResourceResolver {

	Schema getSchema(String id);
	
	Schema getSchemaByUrl(String originalURL);
	
	void addSchema(Schema schema);

	void deleteSchema(Schema schema);

	javax.xml.validation.Schema loadSchema(String url);
	
}
