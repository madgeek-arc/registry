package eu.openminted.registry.core.elasticsearch.service;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
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

	
	private static final Map<String, String> FIELD_TYPES_MAP;
	
	static {
		Map<String, String> unmodifiableMap = new HashMap<String, String>();
		unmodifiableMap.put("java.lang.Double", "double");
		unmodifiableMap.put("java.lang.Integer", "int");
		unmodifiableMap.put("java.lang.Long", "long");
		unmodifiableMap.put("java.lang.String", "string");
		unmodifiableMap.put("java.util.Date", "date");
		FIELD_TYPES_MAP = Collections.unmodifiableMap(unmodifiableMap);
	}
	
	
	
	public void add(Resource resource) {

		TransportClient client = null;
		try {
			client = new PreBuiltTransportClient(Settings.EMPTY)
					.addTransportAddress(new InetSocketTransportAddress(
							InetAddress.getByName(
									environment.getRequiredProperty("elasticsearch.url")),
									Integer.parseInt(environment.getRequiredProperty("elasticsearch.port"))
							)
					);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		
		IndexResponse response;
		response = client.prepareIndex(resource.getResourceType(),"general").setSource(createDocumentForInsert(resource).toString()).setId(resource.getId()).get();
		
	}

	public void update(Resource previousResource, Resource newResource) {
		TransportClient client = null;
		try {
			client = new PreBuiltTransportClient(Settings.EMPTY)
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(
							environment.getRequiredProperty("elasticsearch.url")),
							Integer.parseInt(environment.getRequiredProperty("elasticsearch.port"))
					));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

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
		TransportClient client = null;
		try {
			client = new PreBuiltTransportClient(Settings.EMPTY)
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(
							environment.getRequiredProperty("elasticsearch.url")),
							Integer.parseInt(environment.getRequiredProperty("elasticsearch.port"))
					));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		DeleteResponse response = client.prepareDelete(resource.getResourceType(), "general", resource.getId()).get();
		
	}

	public void createIndex(ResourceType resourceType) {

		TransportClient client = null;
		try {
			client = new PreBuiltTransportClient(Settings.EMPTY)
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(
							environment.getRequiredProperty("elasticsearch.url")),
							Integer.parseInt(environment.getRequiredProperty("elasticsearch.port"))
					));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		
		client.admin().indices().prepareCreate(resourceType.getName()).execute().actionGet();
		
		
		//TO PARAKATW FTIAXNEI TO MAPPING(SCHEMA) GIA TO INDEX
		//PROS TO PARON SXOLIAZETAI 
		
		JSONObject jsonObjectForMapping = createMapping(resourceType.getIndexFields());
		XContentBuilder mapping = null;
		try {
			mapping.startObject(jsonObjectForMapping.toString());
		} catch (IOException e) {
//			new ServiceException(e.getMessage());
			e.printStackTrace();
		}

		PutMappingResponse putMappingResponse = client.admin().indices()
		  .preparePutMapping(resourceType.getName())
		  .setType("general")
		  .setSource(mapping)
		  .execute().actionGet();
		
		client.close();
		
	}

	public JSONObject createMapping(List<IndexField> indexFields){
		
		JSONObject jsonObjectRoot = new JSONObject();
		JSONObject jsonObjectGeneral = new JSONObject();
		JSONObject jsonObjectProperties = new JSONObject();
		
		if (indexFields != null) {
			for (IndexField indexField : indexFields) {
				JSONObject jsonObjectField = new JSONObject();
				jsonObjectField.put("type", FIELD_TYPES_MAP.get(indexField.getType()));
				jsonObjectProperties.put(indexField.getName(), jsonObjectField);
			}
		}
		jsonObjectGeneral.put("properties", jsonObjectProperties);
		jsonObjectRoot.put("general", jsonObjectGeneral);
		
		return jsonObjectRoot;
		
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
