package gr.uoa.di.madgik.registry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchService.class);

    private static final String[] INCLUDES = {"id", "payload", "creation_date", "modification_date", "payloadFormat", "version"};
    private final ObjectMapper mapper;
    @Value("${elastic.aggregation.topHitsSize:100}")
    private int topHitsSize;
    @Value("${elastic.aggregation.bucketSize:100}")
    private int bucketSize;
    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public DefaultSearchService() {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new ResourcePropertyName());
    }

    @Override
    public Paging<Resource> query(FacetFilter filter) {
        validateQuantity(filter.getQuantity());
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Paging<Resource> query(String query,
                                  String resourceType,
                                  int quantity,
                                  int from,
                                  String sortByField,
                                  String sortOrder) {
        validateQuantity(quantity);
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Paging<Resource> query(String query, String resourceType) {
        return query(query, resourceType, 100, 0, "", "ASC");
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    @Retryable(value = ServiceException.class, backoff = @Backoff(value = 200))
    public Resource searchId(String resourceType, KeyValue... ids) throws ServiceException {
        logger.debug(String.format("@Retryable 'searchId(resourceType=%s, ids={%s})'", resourceType, String.join(",", Arrays.stream(ids).map(keyValue -> keyValue.getField() + "=" + keyValue.getValue()).collect(Collectors.toSet()))));
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    private void validateQuantity(int quantity) {
        if (quantity > maxQuantity) {
            throw new IllegalArgumentException(String.format("Quantity should be up to %s.", maxQuantity));
        } else if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    static private class ResourcePropertyName extends PropertyNamingStrategy.PropertyNamingStrategyBase {

        @Override
        public String translate(String propertyName) {
            switch (propertyName) {
                case "modificationDate":
                    return "modification_date";
                case "creationDate":
                    return "creation_date";
                default:
                    return propertyName;
            }
        }
    }
}
