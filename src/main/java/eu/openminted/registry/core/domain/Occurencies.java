package eu.openminted.registry.core.domain;

import java.util.HashMap;
import java.util.Map;

public class Occurencies {
	
	private Map<String, Map<String,Integer>> values = new HashMap<String, Map<String,Integer>>();

	public Map<String, Map<String, Integer>> getValues() {
		return values;
	}

	public void setValues(Map<String, Map<String, Integer>> values) {
		this.values = values;
	}
	
	

}
