/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeProjectionService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Startup consistency check between SQL-backed resource projections and Elasticsearch indices.
 */
public class SearchIndexConsistencyService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexConsistencyService.class);

    private final ElasticsearchClient elasticsearchClient;
    private final ResourceTypeService resourceTypeService;
    private final ResourceTypeProjectionService resourceTypeProjectionService;
    private final ResourceService resourceService;
    private final IndexOperationsService indexOperationsService;
    private final TaskExecutor taskExecutor = new VirtualThreadTaskExecutor();

    public SearchIndexConsistencyService(ElasticsearchClient elasticsearchClient,
                                         ResourceTypeService resourceTypeService,
                                         ResourceTypeProjectionService resourceTypeProjectionService,
                                         ResourceService resourceService,
                                         IndexOperationsService indexOperationsService) {
        this.elasticsearchClient = elasticsearchClient;
        this.resourceTypeService = resourceTypeService;
        this.resourceTypeProjectionService = resourceTypeProjectionService;
        this.resourceService = resourceService;
        this.indexOperationsService = indexOperationsService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reindexOnInit() {
        taskExecutor.execute(this::ensureDatabaseIndexConsistency);
    }

    public void ensureDatabaseIndexConsistency() {
        logger.info("Checking for index inconsistencies");
        resourceTypeService.getAllResourceType()
                .forEach(resourceType -> {
                    String name = resourceType.getName();
                    Set<String> indexIds = fetchResourceIdsFromIndex(name);
                    Set<String> dbIds = fetchResourceIdsFromDatabase(name);
                    reindex(name, indexIds, dbIds);
                    checkDatabaseConsistency(name, indexIds, dbIds);
                });
    }

    public void reindex() {
        resourceTypeService.getAllResourceType().forEach(resourceType -> {
            String name = resourceType.getName();
            reindex(name, fetchResourceIdsFromIndex(name), fetchResourceIdsFromDatabase(name));
        });
    }

    private Set<String> fetchResourceIdsFromDatabase(String resourceType) {
        return new LinkedHashSet<>(resourceTypeProjectionService.fetchResourceIds(resourceType));
    }

    private List<String> findAllResourceIdsFromElasticIndex(String resourceType) {
        List<String> resourceIds = new ArrayList<>();
        String scrollId = null;
        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                            .index(resourceType)
                            .scroll(t -> t.time("1m"))
                            .size(10000)
                            .source(src -> src.fetch(false))
                            .query(q -> q.matchAll(m -> m)),
                    Void.class);
            scrollId = response.scrollId();
            List<Hit<Void>> hits = response.hits().hits();
            while (!hits.isEmpty()) {
                hits.stream().map(Hit::id).forEach(resourceIds::add);
                String currentScrollId = scrollId;
                ScrollResponse<Void> scrollResponse = elasticsearchClient.scroll(
                        sr -> sr.scrollId(currentScrollId).scroll(t -> t.time("1m")),
                        Void.class);
                scrollId = scrollResponse.scrollId();
                hits = scrollResponse.hits().hits();
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to read Elasticsearch index " + resourceType, e);
        } finally {
            clearScroll(scrollId);
        }
        return resourceIds;
    }

    private void clearScroll(String scrollId) {
        if (scrollId == null || scrollId.isBlank()) {
            return;
        }
        try {
            elasticsearchClient.clearScroll(cs -> cs.scrollId(scrollId));
        } catch (Exception e) {
            logger.error("clear scroll request failed", e);
        }
    }

    private Set<String> fetchResourceIdsFromIndex(String resourceType) {
        Set<String> resourceIds = new LinkedHashSet<>();

        if (!exists(resourceType)) {
            logger.warn("Elasticsearch index '{}' is missing. Recreating it from the resource type definition.", resourceType);
            ensureIndexExists(resourceType);
            return resourceIds;
        }

        boolean done = false;
        short retries = 5;
        do {
            try {
                resourceIds = new LinkedHashSet<>(findAllResourceIdsFromElasticIndex(resourceType));
                done = true;
            } catch (ServiceException e) {
                if (isNotFound(e)) {
                    logger.warn("Elasticsearch index '{}' missing. Recreating it from the resource type definition.", resourceType);
                    ensureIndexExists(resourceType);
                    return new LinkedHashSet<>();
                }
                logger.error(e.getMessage(), e);
            }
            retries--;
        } while (!done && retries > 0);
        return resourceIds;
    }

    private boolean exists(String indexName) {
        try {
            return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
        } catch (IOException e) {
            throw new ServiceException("Failed to check index existence for " + indexName, e);
        }
    }

    private boolean isNotFound(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ElasticsearchException esEx && esEx.status() == 404) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void reindex(String resourceType, Set<String> indexResources, Set<String> databaseResources) {
        List<String> missingIndexIds = new ArrayList<>(databaseResources);
        missingIndexIds.removeAll(indexResources);
        if (!missingIndexIds.isEmpty()) {
            logger.debug("Reindexing missing resources [{}] on {}", missingIndexIds, resourceType);
            reindexByIds(missingIndexIds);
        } else {
            logger.debug("Index is consistent with Database on {}", resourceType);
        }
    }

    private void checkDatabaseConsistency(String resourceType, Set<String> indexResources, Set<String> databaseResources) {
        List<String> missingDBIds = new ArrayList<>(indexResources);
        missingDBIds.removeAll(databaseResources);
        if (!missingDBIds.isEmpty()) {
            logger.error("Database is missing the following resources [{}] on {}", missingDBIds, resourceType);
        } else {
            logger.debug("Database is consistent with Index on {}", resourceType);
        }
    }

    private void reindexByIds(List<String> ids) {
        logger.info("Reindexing {} missing resource{}.", ids.size(), ids.size() == 1 ? "" : "s");
        for (String missingId : ids) {
            Resource resource = resourceService.getResourceForIndexing(missingId);
            logger.trace("Adding resource with id '{}' to index '{}'", resource.getId(), resource.getResourceTypeName());
            indexOperationsService.add(resource);
        }
        logger.info("Reindexing finished.");
    }

    private void ensureIndexExists(String resourceType) {
        ResourceType definition = resourceTypeService.getResourceType(resourceType);
        if (definition == null) {
            throw new ServiceException("Cannot create missing index for unknown resource type " + resourceType);
        }
        indexOperationsService.createIndex(definition);
    }
}
