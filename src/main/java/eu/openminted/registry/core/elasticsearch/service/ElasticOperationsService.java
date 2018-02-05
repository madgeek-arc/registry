package eu.openminted.registry.core.elasticsearch.service;

import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service("elasticOperationsService")
@Transactional
public class ElasticOperationsService {

    private static Logger logger = LogManager.getLogger(ElasticOperationsService.class);

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

        long start_time = System.nanoTime();
        Client client = elastic.client();
        String payload = createDocumentForInsert(resource);
        client.prepareIndex(resource.getResourceType().getName(), "general")
                .setSource(payload)
                .setId(resource.getId()).get();
        long end_time = System.nanoTime();
        double difference = (end_time - start_time) / 1e6;
        logger.info("Resource added in "+difference+"ms to Elastic");
    }

    public void update(Resource previousResource, Resource newResource) {
        Client client = elastic.client();

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(newResource.getResourceType().getName());
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
        long start_time = System.nanoTime();
        Client client = elastic.client();
        client.prepareDelete(resource.getResourceType().getName(), "general", resource.getId()).get();
        long end_time = System.nanoTime();
        double difference = (end_time - start_time) / 1e6;

        logger.info("Resource deleted in "+difference+"ms from Elastic");

    }

    public void createIndex(ResourceType resourceType) {
        long start_time = System.nanoTime();
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
        long end_time = System.nanoTime();
        double difference = (end_time - start_time) / 1e6;
        logger.info("Resource type "+resourceType.getName()+" added in "+difference+"ms to Elastic");
    }

    public void deleteIndex(String name) {
        long start_time = System.nanoTime();
        System.out.println("Deleting index");

        Client client = elastic.client();
        DeleteIndexResponse deleteResponse = client.admin().indices().delete(new DeleteIndexRequest(name)).actionGet();

        if(!deleteResponse.isAcknowledged()){
            System.err.println("Error deleting index \""+name+"\"");
        }
        long end_time = System.nanoTime();
        double difference = (end_time - start_time) / 1e6;
        logger.info("Resource type "+name+" deleted in "+difference+"ms from Elastic");
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
        jsonObjectField.put("resourceType", resource.getResourceType().getName());
        jsonObjectField.put("payload", resource.getPayload());
        jsonObjectField.put("payloadFormat", resource.getPayloadFormat());
        jsonObjectField.put("version", resource.getVersion());
        jsonObjectField.put("searchableArea", strip(resource.getPayload(),resource.getPayloadFormat()));
        jsonObjectField.put("modification_date", resource.getModificationDate().getTime());
        Map<String,IndexField> indexMap = resourceTypeService.getResourceTypeIndexFields(
                resource.getResourceType().getName()).
                stream().collect(Collectors.toMap(IndexField::getName, p->p)
        );
        if (resource.getIndexedFields() != null) {
            for (IndexedField<?> field : resource.getIndexedFields()) {
                if(!indexMap.get(field.getName()).isMultivalued()) {
                    for (Object value : field.getValues()) {
                        jsonObjectField.put(field.getName(), value);
                    }
                } else {
                    List<String> values = new ArrayList<>();
                    values.addAll(field.getValues());
                    jsonObjectField.put(field.getName(),values);
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
