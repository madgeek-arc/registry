package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import org.everit.json.schema.loader.SchemaClient;
import org.w3c.dom.ls.LSResourceResolver;

public interface SchemaDao extends LSResourceResolver, SchemaClient {

    Schema getSchema(String id);

    Schema getSchemaByUrl(String originalURL);

    String replaceLastSegment(String url, String replacingPath);

    void addSchema(Schema schema);

    void deleteSchema(Schema schema);

    javax.xml.validation.Schema loadXMLSchema(ResourceType url);

    org.everit.json.schema.Schema loadJSONSchema(ResourceType url);

}
