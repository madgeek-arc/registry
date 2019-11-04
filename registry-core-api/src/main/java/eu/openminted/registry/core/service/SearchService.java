package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import org.elasticsearch.index.query.QueryBuilder;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;


public interface SearchService {

	QueryBuilder createQueryBuilder(FacetFilter facetFilter);

	Paging<Resource> cqlQuery(String query, String resourceType, int quantity,int from, String sortByField, String sortOrder);

	Paging<Resource> cqlQuery(String query, String resourceType);

    Paging<Resource> cqlQuery(FacetFilter filter);

	Paging<Resource> search(FacetFilter filter) throws ServiceException, UnknownHostException;

	Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException, UnknownHostException;

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


