package eu.openminted.registry.core.elasticsearch.service;

import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    @Value("${prefix:general}")
    private String type;

    private static final Map<String, String> FIELD_TYPES_MAP;

    static {
        Map<String, String> unmodifiableMap = new HashMap<>();
        unmodifiableMap.put("java.lang.Float", "float");
        unmodifiableMap.put("java.lang.Integer", "integer");
        unmodifiableMap.put("java.lang.Boolean", "boolean");
        unmodifiableMap.put("java.lang.Long", "long");
        unmodifiableMap.put("java.lang.String", "keyword");
        unmodifiableMap.put("java.util.Date", "date");
        FIELD_TYPES_MAP = Collections.unmodifiableMap(unmodifiableMap);
    }

    public void addBulk(List<Resource> resources){
        Client client = elastic.client();
        BulkRequestBuilder bulkRequest = client.prepareBulk();


        for(Resource resource : resources){
            bulkRequest.add(client.prepareIndex(resource.getResourceType().getName(), type)
                        .setSource(createDocumentForInsert(resource))
                        .setId(resource.getId()));
        }

        logger.info("Sending bulk request for " + resources.size() + " resources");
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        BulkResponse bulkResponse = bulkRequest.get();

        if(bulkResponse.hasFailures()){
            logger.info("Elastic bulk request ended up with some errors");
        }
    }

    public void add(Resource resource) {
        Client client = elastic.client();
        String payload = createDocumentForInsert(resource);
        IndexResponse response = client.prepareIndex(resource.getResourceType().getName(), type)
                .setSource(payload)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setId(resource.getId()).get();
    }

    public void update(Resource previousResource, Resource newResource) {
        Client client = elastic.client();

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(newResource.getResourceType().getName());
        updateRequest.type(type);
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
        client.prepareDelete(resource.getResourceType().getName(), type, resource.getId()).get();
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

        createIndexRequestBuilder.addMapping(type, jsonObjectForMapping);

        CreateIndexResponse putMappingResponse = createIndexRequestBuilder.get();

        if (!putMappingResponse.isAcknowledged()) {
            System.err.println("Error creating result");
        }
        long end_time = System.nanoTime();
        double difference = (end_time - start_time) / 1e6;
        logger.info("Resource type "+resourceType.getName()+" added in "+difference+"ms to Elastic");
    }

    public void deleteIndex(String name) {
        logger.info("Deleting index");

        Client client = elastic.client();
        DeleteIndexResponse deleteResponse = client.admin().indices().delete(new DeleteIndexRequest(name)).actionGet();

        if(!deleteResponse.isAcknowledged()){
            logger.fatal("Error deleting index \""+name+"\"");
        }
    }

    private Map<String, Object> createMapping(List<IndexField> indexFields) {

        Map<String, Object> jsonObjectGeneral = new HashMap<>();
        Map<String, Object> jsonObjectProperties = new HashMap<>();
        if (indexFields != null) {
            for (IndexField indexField : indexFields) {
                Map<String, Object> typeMap = new HashMap<>();
                typeMap.put("type", FIELD_TYPES_MAP.get(indexField.getType()));
                if(indexField.getType().equals("java.util.Date"))
                    typeMap.put("format","epoch_millis");
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
        //The creation date exists and should not be updated
        if(resource.getCreationDate() != null) {
            jsonObjectField.put("creation_date", resource.getCreationDate().getTime());
        }
        Map<String,IndexField> indexMap = resourceTypeService.getResourceTypeIndexFields(
                resource.getResourceType().getName()).
                stream().collect(Collectors.toMap(IndexField::getName, p->p)
        );
        if (resource.getIndexedFields() != null) {
            for (IndexedField<?> field : resource.getIndexedFields()) {
                if(!indexMap.get(field.getName()).isMultivalued()) {
                    for (Object value : field.getValues()) {
                        String fieldType = indexMap.get(field.getName()).getType();
                        if(fieldType.equals("java.lang.String")){
                            jsonObjectField.put(field.getName(), value);
                        }else if(fieldType.equals("java.lang.Integer")){
                            jsonObjectField.put(field.getName(),value);
                        }else if(fieldType.equals("java.lang.Float")){
                            jsonObjectField.put(field.getName(),value);
                        }else if(fieldType.equals("java.util.Date")){
                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                            try {
                                jsonObjectField.put(field.getName(), sdf.parse(sdf.format((Date) value)).getTime());
                            } catch (ParseException e) {
                                throw new ServiceException("Wrong date format for indexed field. Try dd-MM-yyyy") ;
                            }

                        }else if (fieldType.equals("java.lang.Boolean")){
                            jsonObjectField.put(field.getName(),value);
                        }
                    }
                } else {
                    List<Object> values = new ArrayList<>();
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
