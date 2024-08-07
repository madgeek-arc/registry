package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;


public interface SearchService {

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType, int quantity, int from, String sortByField, String sortOrder);

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType);

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> search(FacetFilter filter) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category);

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