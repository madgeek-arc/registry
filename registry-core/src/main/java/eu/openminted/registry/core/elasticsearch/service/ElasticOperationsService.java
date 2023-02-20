package eu.openminted.registry.core.elasticsearch.service;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service("elasticOperationsService")
@Transactional
public class ElasticOperationsService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticOperationsService.class);

    private final ResourceTypeService resourceTypeService;
    private final RestHighLevelClient client;

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

    public ElasticOperationsService(ResourceTypeService resourceTypeService, RestHighLevelClient client) {
        this.resourceTypeService = resourceTypeService;
        this.client = client;
    }

    public void addBulk(List<Resource> resources){
        BulkRequest bulkRequest = new BulkRequest();


        for(Resource resource : resources){
            bulkRequest.add(new IndexRequest(resource.getResourceType().getName())
                        .source(createDocumentForInsert(resource),XContentType.JSON)
                        .id(resource.getId()));
        }

        logger.info("Sending bulk request for {} resources", resources.size());
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        BulkResponse bulkResponse = null;
        try {
            bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Elastic bulk request ended up with some errors");
        }

        if(bulkResponse.hasFailures()){
            logger.error("Elastic bulk request ended up with some errors");
        }
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void add(Resource resource) {
        String payload = createDocumentForInsert(resource);

        IndexRequest indexRequest = new IndexRequest(resource.getResourceType().getName());
        indexRequest.id(resource.getId());
        indexRequest.source(payload,XContentType.JSON);
        indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        try {
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void update(Resource previousResource, Resource newResource) {
        UpdateRequest updateRequest = new UpdateRequest();

        updateRequest.index(newResource.getResourceType().getName());
        updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        updateRequest.id(previousResource.getId());
        updateRequest.doc(createDocumentForInsert(newResource),XContentType.JSON);
        try {
            client.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void delete(Resource resource) {
        DeleteRequest deleteRequest = new DeleteRequest(resource.getResourceType().getName(), resource.getId());
        try {
            client.delete(deleteRequest,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void createIndex(ResourceType resourceType) {
        if (exists(resourceType.getName())) {
            return;
        }

        CreateIndexRequest request = new CreateIndexRequest(resourceType.getName());

        if (resourceType.getAliasGroup() != null) {
            request.alias(new Alias(resourceType.getAliasGroup()));
        }

        Map<String, Object> jsonObjectForMapping = createMapping(resourceType.getIndexFields());

        JSONObject parameters = new JSONObject(jsonObjectForMapping);
        if (logger.isDebugEnabled()) {
            logger.debug(parameters.toString(2));
        }

        request.mapping(jsonObjectForMapping);

        try {
            CreateIndexResponse putMappingResponse = client.indices().create(request,RequestOptions.DEFAULT); //request, RequestOptions.DEFAULT);
            if (!putMappingResponse.isAcknowledged()) {
                logger.error("Error creating result");
            }
        } catch (IOException e) {
           throw new ServiceException(e);
        }

    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void deleteIndex(String name) {
        logger.info("Deleting index");

        if(!exists(name)) {
            return;
        }
        AcknowledgedResponse deleteResponse = null;
        try {
            deleteResponse = client.indices().delete(new DeleteIndexRequest(name), RequestOptions.DEFAULT);
            if(!deleteResponse.isAcknowledged()){
                logger.error("Error deleting index \"{}\"", name);
            }
        } catch (IOException e) {
            logger.error("Error deleting index: " + name,e);
        }


    }

    private boolean exists(String indexName) {
        try {
            GetIndexRequest request = new GetIndexRequest(indexName);
            boolean result = client.indices().exists(request, RequestOptions.DEFAULT);
            logger.info("Existence of index '{}' result is: {}", indexName, result);
            return result;
        } catch (IOException e) {
            logger.error("Exception at waiting for IndicesExistsResponse", e);
            return false;
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
                        }else if(fieldType.equals("java.lang.Long")){
                            jsonObjectField.put(field.getName(),value);
                        }else if(fieldType.equals("java.lang.Float")){
                            jsonObjectField.put(field.getName(),value);
                        }else if(fieldType.equals("java.util.Date")){
                            Date date = (Date) value;
                            jsonObjectField.put(field.getName(), date.getTime());
                        }else if (fieldType.equals("java.lang.Boolean")){
                            jsonObjectField.put(field.getName(),value);
                        }
                    }
                } else {
                    List<Object> values = new ArrayList<>(field.getValues());
                    jsonObjectField.put(field.getName(),values);

                }
            }
        }
        return jsonObjectField.toString();
    }

    private static String strip(String input, String format) {
        if ( "xml".equals(format)) {
            return input.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
        } else if ("json".equals(format)) {
            return input;
        } else {
            throw new ServiceException("Invalid format type, supported are json and xml");
        }
    }
}
