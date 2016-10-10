package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Schema;

public interface SchemaDao {

	Schema getSchema(String id);
	
	Schema getSchemaByUrl(String originalURL);
	
	void addSchema(Schema schema);
	
}
