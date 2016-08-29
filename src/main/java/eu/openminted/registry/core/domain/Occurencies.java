package eu.openminted.registry.core.domain;

import java.util.Map;

public class Occurencies {
	
	private String resourceType;
	
	private Map<String, Map<String,String>> values;

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public Map<String, Map<String, String>> getValues() {
		return values;
	}

	public void setValues(Map<String, Map<String, String>> values) {
		this.values = values;
	}
	
	

}
