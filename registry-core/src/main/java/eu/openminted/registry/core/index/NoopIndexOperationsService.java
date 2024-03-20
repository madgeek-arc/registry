package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.IndexOperationsService;
import eu.openminted.registry.core.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
@Transactional
public class NoopIndexOperationsService implements IndexOperationsService {

    private static final Logger logger = LoggerFactory.getLogger(NoopIndexOperationsService.class);

    @PostConstruct
    void test() {
        logger.info("test no-op");
    }

    public NoopIndexOperationsService() {
    }

    public void addBulk(List<Resource> resources) {
        // no-op
        logger.debug("addBulk() : no-op");
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void add(Resource resource) {
        // no-op
        logger.debug("add() : no-op");
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void update(Resource previousResource, Resource newResource) {
        // no-op
        logger.debug("update() : no-op");
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void delete(String resourceId, String resourceType) {
        // no-op
        logger.debug("delete() : no-op");
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void delete(Resource resource) {
        // no-op
        logger.debug("delete() : no-op");
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void createIndex(ResourceType resourceType) {
        // no-op
        logger.debug("createIndex() : no-op");
    }

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void deleteIndex(String name) {
        // no-op
        logger.debug("deleteIndex() : no-op");
    }
}
