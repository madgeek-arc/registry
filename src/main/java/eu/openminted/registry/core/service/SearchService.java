package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import eu.openminted.registry.core.domain.Resource;



public interface SearchService {

	Paging search(FacetFilter filter) throws ServiceException, UnknownHostException;

	Paging searchKeyword(String resourceType, String keyword) throws ServiceException, UnknownHostException;

	Resource searchId(String resourceType, KeyValue... ids) throws ServiceException, UnknownHostException;

	Map<String,List<Resource>> searchByCategory(FacetFilter filter, String category);

	class KeyValue {

		public String field;

		public String value;

		public KeyValue(String field, String value) {
			this.field = field;
			this.value = value;
		}

		public KeyValue() {
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}


