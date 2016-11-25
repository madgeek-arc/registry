package eu.openminted.registry.core.elasticsearch.service;

import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.commons.collections.map.HashedMap;
import org.elasticsearch.ElasticsearchCorruptionException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
	private Environment environment;

	@Autowired
	private ElasticConfiguration elastic;

	private static final Map<String, String> FIELD_TYPES_MAP;
	private static final String COMMON_ALIAS = "resourceTypes";
	
	static {
		Map<String, String> unmodifiableMap = new HashMap<String, String>();
		unmodifiableMap.put("java.lang.Double", "double");
		unmodifiableMap.put("java.lang.Integer", "int");
		unmodifiableMap.put("java.lang.Long", "long");
		unmodifiableMap.put("java.lang.String", "keyword");
		unmodifiableMap.put("java.util.Date", "date");
		FIELD_TYPES_MAP = Collections.unmodifiableMap(unmodifiableMap);
	}

	public void add(Resource resource) {

		Client client = elastic.client();

		
		IndexResponse response;
		response = client.prepareIndex(resource.getResourceType(),"general").setSource(createDocumentForInsert(resource).toString()).setId(resource.getId()).get();
		
	}

	public void update(Resource previousResource, Resource newResource) {
		Client client = elastic.client();

		UpdateRequest updateRequest = new UpdateRequest();
		updateRequest.index(newResource.getResourceType());
		updateRequest.type("general");
		updateRequest.id(previousResource.getId());
		updateRequest.doc(createDocumentForInsert(newResource).toString());
		try {
			client.update(updateRequest).get();
		} catch (InterruptedException e) {
			new ServiceException(e.getMessage());
			return;
		} catch (ExecutionException e) {
			new ServiceException(e.getMessage());
			return;
		}
	}

	public void delete(Resource resource) {
		Client client = elastic.client();
		
		DeleteResponse response = client.prepareDelete(resource.getResourceType(), "general", resource.getId()).get();
		
	}

	public void createIndex(ResourceType resourceType) {

		Client client = elastic.client();


		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(resourceType.getName());
		createIndexRequestBuilder.addAlias(new Alias(COMMON_ALIAS));
		
		Map<String,Object> jsonObjectForMapping = createMapping(resourceType.getIndexFields());

		JSONObject parameters = new JSONObject(jsonObjectForMapping);
		System.err.println(parameters.toString(2));

		createIndexRequestBuilder.addMapping("general",jsonObjectForMapping);

		CreateIndexResponse putMappingResponse = createIndexRequestBuilder.get();


		if(!putMappingResponse.isAcknowledged()) {
			System.err.println("Error creating result");
		}
		
		//client.close();
		
	}

	public Map<String,Object> createMapping(List<IndexField> indexFields){

		Map<String,Object> jsonObjectGeneral = new HashMap<>();
		Map<String,Object> jsonObjectProperties = new HashMap<>();
		
		if (indexFields != null) {
			for (IndexField indexField : indexFields) {
				Map<String,Object> typeMap = new HashMap<>();
				typeMap.put("type", FIELD_TYPES_MAP.get(indexField.getType()));
				jsonObjectProperties.put(indexField.getName(), typeMap);
			}
		}

		final Map<String,Object> typeMap = new HashMap<>();
		typeMap.put("type","keyword");
		jsonObjectProperties.put("resourceType",typeMap);


		jsonObjectGeneral.put("properties", jsonObjectProperties);
		return jsonObjectGeneral;
		
	}
	
	public JSONObject createDocumentForInsert(Resource resource){
		
		String type = resource.getResourceType();
		ResourceType resourceType = resourceTypeService.getResourceType(type);
		
		JSONObject jsonObjectField = new JSONObject();
		jsonObjectField.put("id", resource.getId());
		jsonObjectField.put("resourceType", type);
		jsonObjectField.put("payload", resource.getPayload());
		jsonObjectField.put("version", resource.getVersion());
		jsonObjectField.put("creation_date", resource.getCreationDate());
		jsonObjectField.put("modification_date", resource.getModificationDate());
		
		if (resource.getIndexedFields() != null) {
			for (IndexedField<?> field:resource.getIndexedFields()) {
				for (Object value:field.getValues()) {
					jsonObjectField.put(field.getName(), getValue(field.getType(), value));
				}
			}
		}
		
		return jsonObjectField;
		
	}
	
	private Object getValue(String type, Object value) {

		// TODO return value properly formatted
		return value;
	}
	
	public String XMLtoJSON(String xmlContent){
		 try {
	            JSONObject xmlJSONObj = XML.toJSONObject(xmlContent);
	            String jsonPrettyPrintString = xmlJSONObj.toString(4);
	            System.out.println(jsonPrettyPrintString);
	            return jsonPrettyPrintString;
	        } catch (JSONException je) {
	            System.out.println(je.toString());
	            return null;
	        }
	}
}
