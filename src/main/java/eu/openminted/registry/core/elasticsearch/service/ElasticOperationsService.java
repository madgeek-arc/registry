package eu.openminted.registry.core.elasticsearch.service;

import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service("elasticOperationsService")
@Transactional
public class ElasticOperationsService {

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    private ElasticConfiguration elastic;

    private static final Map<String, String> FIELD_TYPES_MAP;

    static {
        Map<String, String> unmodifiableMap = new HashMap<>();
        unmodifiableMap.put("java.lang.Double", "double");
        unmodifiableMap.put("java.lang.Integer", "int");
        unmodifiableMap.put("java.lang.Boolean", "boolean");
        unmodifiableMap.put("java.lang.Long", "long");
        unmodifiableMap.put("java.lang.String", "keyword");
        unmodifiableMap.put("java.util.Date", "date");
        FIELD_TYPES_MAP = Collections.unmodifiableMap(unmodifiableMap);
    }

    public void add(Resource resource) {

        Client client = elastic.client();
        String payload = createDocumentForInsert(resource);
        client.prepareIndex(resource.getResourceType(), "general")
                .setSource(payload)
                .setId(resource.getId()).get();
    }

    public void update(Resource previousResource, Resource newResource) {
        Client client = elastic.client();

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(newResource.getResourceType());
        updateRequest.type("general");
        updateRequest.id(previousResource.getId());
        updateRequest.doc(createDocumentForInsert(newResource));
        try {
            client.update(updateRequest).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    public void delete(Resource resource) {
        Client client = elastic.client();
        client.prepareDelete(resource.getResourceType(), "general", resource.getId()).get();
    }

    public void createIndex(ResourceType resourceType) {

        Client client = elastic.client();

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(resourceType.getName());
        if (resourceType.getAliasGroup() != null) {
            createIndexRequestBuilder.addAlias(new Alias(resourceType.getAliasGroup()));
        }

        Map<String, Object> jsonObjectForMapping = createMapping(resourceType.getIndexFields());

        JSONObject parameters = new JSONObject(jsonObjectForMapping);
        System.err.println(parameters.toString(2));

        createIndexRequestBuilder.addMapping("general", jsonObjectForMapping);

        CreateIndexResponse putMappingResponse = createIndexRequestBuilder.get();

        if (!putMappingResponse.isAcknowledged()) {
            System.err.println("Error creating result");
        }

    }

    public void deleteIndex(ResourceType resourceType) {

        Client client = elastic.client();
        DeleteIndexRequestBuilder deleteIndexRequestBuilder = client.admin().indices().prepareDelete(resourceType.getName());

        DeleteIndexResponse deleteIndexResponse = deleteIndexRequestBuilder.get();

        if(!deleteIndexResponse.isAcknowledged()){
            System.err.println("Error deleting index \""+resourceType.getName()+"\"");
        }

    }

    private Map<String, Object> createMapping(List<IndexField> indexFields) {

        Map<String, Object> jsonObjectGeneral = new HashMap<>();
        Map<String, Object> jsonObjectProperties = new HashMap<>();

        if (indexFields != null) {
            for (IndexField indexField : indexFields) {
                Map<String, Object> typeMap = new HashMap<>();
                typeMap.put("type", FIELD_TYPES_MAP.get(indexField.getType()));
                jsonObjectProperties.put(indexField.getName(), typeMap);
            }
        }

        final Map<String, Object> typeMap = new HashMap<>();
        typeMap.put("type", "keyword");
        final Map<String, Object> dateMap = new HashMap<>();
        dateMap.put("type", "date");
        dateMap.put("format", "epoch_millis");
        final Map<String, Object> textMap = new HashMap<>();
        textMap.put("type", "text");

        jsonObjectProperties.put("id", typeMap);
        jsonObjectProperties.put("version", typeMap);
        jsonObjectProperties.put("payload", textMap);
        jsonObjectProperties.put("searchableArea", textMap);
        jsonObjectProperties.put("payloadFormat", typeMap);
        jsonObjectProperties.put("resourceType", typeMap);
        jsonObjectProperties.put("creation_date",dateMap);
        jsonObjectProperties.put("modification_date",dateMap);

        jsonObjectGeneral.put("properties", jsonObjectProperties);
        return jsonObjectGeneral;

    }

    private String createDocumentForInsert(Resource resource) {

        JSONObject jsonObjectField = new JSONObject();
        jsonObjectField.put("id", resource.getId());
        jsonObjectField.put("resourceType", resource.getResourceType());
        jsonObjectField.put("payload", resource.getPayload());
        jsonObjectField.put("payloadFormat", resource.getPayloadFormat());
        jsonObjectField.put("version", resource.getVersion());
        jsonObjectField.put("searchableArea", strip(resource.getPayload(),resource.getPayloadFormat()));
        jsonObjectField.put("modification_date", resource.getModificationDate().getTime());

        if (resource.getIndexedFields() != null) {
            for (IndexedField<?> field : resource.getIndexedFields()) {
                for (Object value : field.getValues()) {
                    jsonObjectField.put(field.getName(), value);
                }
            }
        }
        return jsonObjectField.toString();
    }

    static private String strip(String input, String format) {
        if ( "xml".equals(format)) {
            return input.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
        } else if ("json".equals(format)) {
            return input;
        } else {
            throw new ServiceException("Invalid format type, supported are json and xml");
        }
    }
}
