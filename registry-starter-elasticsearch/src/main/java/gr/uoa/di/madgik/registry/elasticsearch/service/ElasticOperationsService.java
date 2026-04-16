/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.Alias;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunk;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunker;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingSegmenter;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Segment;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gr.uoa.di.madgik.registry.service.EmbeddingService.VECTOR_SIZE;

/**
 * Elasticsearch-backed implementation of {@link IndexOperationsService}.
 *
 * <p>This class preserves the registry's existing indexing contract while issuing typed requests
 * through the official Elasticsearch Java client 9.x
 * ({@code co.elastic.clients:elasticsearch-java}).
 */
@Transactional
public class ElasticOperationsService implements IndexOperationsService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticOperationsService.class);
    private static final Map<String, String> FIELD_TYPES_MAP;

    static {
        Map<String, String> classToTypeMap = new HashMap<>();
        classToTypeMap.put("java.lang.Float", "float");
        classToTypeMap.put("java.lang.Integer", "integer");
        classToTypeMap.put("java.lang.Boolean", "boolean");
        classToTypeMap.put("java.lang.Long", "long");
        classToTypeMap.put("java.lang.String", "keyword");
        classToTypeMap.put("java.util.Date", "date");
        classToTypeMap.put("java.time.Instant", "date");
        classToTypeMap.put("embedding", "dense_vector");
        FIELD_TYPES_MAP = Collections.unmodifiableMap(classToTypeMap);
    }

    private static final Map<String, Object> KEYWORD_MAP = Map.of("type", "keyword");
    private static final Map<String, Object> INTEGER_MAP = Map.of("type", "integer");
    private static final Map<String, Object> DATE_MAP = Map.of("type", "date", "format", "strict_date_optional_time||epoch_millis");
    private static final Map<String, Object> TEXT_MAP = Map.of("type", "text");
    private static final Map<String, Object> DENSE_VECTOR_MAP = Map.of(
            "type", "dense_vector",
            "dims", VECTOR_SIZE,
            "index", true,
            "similarity", "cosine"
    );
    private static final Map<String, Object> CHUNK_EMBEDDINGS_MAP = Map.of(
            "type", "nested",
            "properties", Map.of(
                    "field", KEYWORD_MAP,
                    "value_ordinal", INTEGER_MAP,
                    "field_chunk_idx", INTEGER_MAP,
                    "content", TEXT_MAP,
                    "embedding", DENSE_VECTOR_MAP
            )
    );

    private final ResourceTypeService resourceTypeService;
    private final ResourceService resourceService;
    private final ElasticsearchClient client;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    /**
     * Creates an indexing service backed by the typed Elasticsearch Java client.
     */
    public ElasticOperationsService(ResourceTypeService resourceTypeService, ResourceService resourceService,
                                    ElasticsearchClient client, EmbeddingService embeddingService,
                                    ObjectMapper objectMapper) {
        this.resourceTypeService = resourceTypeService;
        this.resourceService = resourceService;
        this.client = client;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Produces a searchable plain-text representation from the stored payload.
     */
    private static String strip(String input, String format) {
        if ("xml".equals(format)) {
            return input.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
        } else if ("json".equals(format)) {
            return input;
        } else {
            throw new ServiceException("Invalid format type, supported are json and xml");
        }
    }

    @Override
    public void addBulk(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            return;
        }
        try {
            List<BulkOperation> ops = new ArrayList<>();
            for (Resource resource : resources) {
                Map<String, Object> doc = createDocumentForInsert(resource);
                String indexName = resource.getResourceType().getName();
                String resourceId = resource.getId();
                ops.add(BulkOperation.of(op -> op.index(idx -> idx
                        .index(indexName).id(resourceId).document(doc))));
            }
            logger.info("Sending bulk request for {} resources", resources.size());
            client.bulk(b -> b.operations(ops).refresh(Refresh.True));
        } catch (IOException e) {
            throw new ServiceException("Elastic bulk request failed", e);
        }
    }

    @Override
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void add(Resource resource) {
        try {
            Map<String, Object> doc = createDocumentForInsert(resource);
            client.index(i -> i
                    .index(resource.getResourceType().getName())
                    .id(resource.getId())
                    .document(doc)
                    .refresh(Refresh.True));
        } catch (IOException e) {
            throw new ServiceException("Failed to index resource " + resource.getId(), e);
        }
    }

    @Override
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void update(Resource previousResource, Resource newResource) {
        try {
            Map<String, Object> newDoc = createDocumentForInsert(newResource);
            client.update(u -> u
                    .index(newResource.getResourceType().getName())
                    .id(previousResource.getId())
                    .doc(newDoc)
                    .refresh(Refresh.True),
                    Map.class);
        } catch (IOException e) {
            throw new ServiceException("Failed to update resource " + previousResource.getId(), e);
        }
    }

    @Override
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void delete(String resourceId, String resourceType) {
        try {
            client.delete(d -> d.index(resourceType).id(resourceId).refresh(Refresh.True));
        } catch (IOException e) {
            throw new ServiceException("Failed to delete resource " + resourceId, e);
        }
    }

    @Override
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void delete(Resource resource) {
        delete(resource.getId(), resource.getResourceType().getName());
    }

    @Override
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void createIndex(ResourceType resourceType) {
        if (exists(resourceType.getName())) {
            return;
        }

        try {
            Map<String, Alias> aliases = new LinkedHashMap<>();
            if (resourceType.getAliases() != null) {
                for (String alias : resourceType.getAliases()) {
                    aliases.put(alias, Alias.of(a -> a));
                }
            }

            String mappingJson = objectMapper.writeValueAsString(createMappingAsMap(resourceType.getIndexFields()));
            final Map<String, Alias> finalAliases = aliases;
            client.indices().create(c -> c
                    .index(resourceType.getName())
                    .aliases(finalAliases)
                    .mappings(m -> m.withJson(new StringReader(mappingJson))));
        } catch (IOException e) {
            throw new ServiceException("Failed to create index " + resourceType.getName(), e);
        }
    }

    @Override
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void updateIndex(ResourceType previous, ResourceType updated) {
        deleteIndex(updated.getName());
        createIndex(updated);
        addBulk(resourceService.getResource(updated));
    }

    @Override
    @Retryable(retryFor = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    public void deleteIndex(String name) {
        logger.info("Deleting index");

        if (!exists(name)) {
            return;
        }

        try {
            client.indices().delete(d -> d.index(name));
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() != 404) {
                throw new ServiceException("Error deleting index: " + name, e);
            }
        } catch (IOException e) {
            throw new ServiceException("Error deleting index: " + name, e);
        }
    }

    /**
     * Checks whether an Elasticsearch index already exists.
     */
    private boolean exists(String indexName) {
        try {
            return client.indices().exists(e -> e.index(indexName)).value();
        } catch (IOException e) {
            throw new ServiceException("Failed to check index existence for " + indexName, e);
        }
    }

    /**
     * Builds Elasticsearch mapping JSON from registry {@link IndexField} metadata.
     */
    private Map<String, Object> createMappingAsMap(List<IndexField> indexFields) {

        Map<String, Object> jsonObjectGeneral = new HashMap<>();
        Map<String, Object> jsonObjectProperties = new HashMap<>();
        if (indexFields != null) {
            for (IndexField indexField : indexFields) {
                Map<String, Object> typeMap = new HashMap<>();
                typeMap.put("type", FIELD_TYPES_MAP.get(indexField.getType()));
                switch (indexField.getType()) {
                    case "java.lang.String" -> {
                        if (indexField.hasSearchCapability(SearchCapability.TEXT)) {
                            typeMap.put("type", "text");
                            if (indexField.hasSearchCapability(SearchCapability.KEYWORD)) {
                                typeMap.put("fields", Map.of("keyword", KEYWORD_MAP));
                            }
                        } else {
                            typeMap.put("fields", Map.of("analyzed", TEXT_MAP));
                        }
                    }
                    case "embedding" -> typeMap.put("dims", VECTOR_SIZE);
                    default -> {
                    }
                }
                jsonObjectProperties.put(indexField.getName(), typeMap);
            }
        }

        jsonObjectProperties.put("id", KEYWORD_MAP);
        jsonObjectProperties.put("version", KEYWORD_MAP);
        jsonObjectProperties.put("payload", TEXT_MAP);
        jsonObjectProperties.put("searchableArea", TEXT_MAP);
        jsonObjectProperties.put("payloadFormat", KEYWORD_MAP);
        jsonObjectProperties.put("resourceType", KEYWORD_MAP);
        jsonObjectProperties.put("creation_date", DATE_MAP);
        jsonObjectProperties.put("modification_date", DATE_MAP);
        jsonObjectProperties.put("created_by", KEYWORD_MAP);
        jsonObjectProperties.put("modified_by", KEYWORD_MAP);
        jsonObjectProperties.put("embedding", DENSE_VECTOR_MAP);
        // Experimental only: chunk vectors are stored in Elasticsearch for inspection and future work,
        // but the active ES search path still queries only the resource-level "embedding" field.
        jsonObjectProperties.put("chunk_embeddings", CHUNK_EMBEDDINGS_MAP);

        jsonObjectGeneral.put("properties", jsonObjectProperties);
        jsonObjectGeneral.put("_source", Map.of("excludes", List.of("embedding")));
//        jsonObjectGeneral.put("_source", Map.of("excludes", List.of("embedding", "chunk_embeddings.embedding")));

        return jsonObjectGeneral;
    }

    /**
     * Converts a registry {@link Resource} into the JSON document stored in Elasticsearch.
     *
     * <p>Besides raw payload fields, this normalizes temporal values to epoch millis, derives the
     * plain-text searchable area, and adds an embedding when the resource type marks fields as
     * embedding contributors.</p>
     */
    private Map<String, Object> createDocumentForInsert(Resource resource) {

        Map<String, Object> jsonObjectField = new LinkedHashMap<>();
        jsonObjectField.put("id", resource.getId());
        jsonObjectField.put("resourceType", resource.getResourceType().getName());
        jsonObjectField.put("payload", resource.getPayload());
        jsonObjectField.put("payloadFormat", resource.getPayloadFormat());
        jsonObjectField.put("version", resource.getVersion());
        jsonObjectField.put("searchableArea", strip(resource.getPayload(), resource.getPayloadFormat()));
        jsonObjectField.put("modification_date", resource.getModificationDate().toString());
        jsonObjectField.put("created_by", resource.getCreatedBy());
        jsonObjectField.put("modified_by", resource.getModifiedBy());
        // Experimental mirror of the SQL chunk index. These nested chunk vectors are not used by the
        // current Elasticsearch SearchService implementation, which ranks documents by resource embedding.
        jsonObjectField.put("chunk_embeddings", createChunkEmbeddings(resource));
        //The creation date exists and should not be updated
        if (resource.getCreationDate() != null) {
            jsonObjectField.put("creation_date", resource.getCreationDate().toString());
        }
        Map<String, IndexField> indexMap = resourceTypeService.getResourceTypeIndexFields(
                        resource.getResourceType().getName()).
                stream().collect(Collectors.toMap(IndexField::getName, p -> p)
                );
        if (resource.getIndexedFields() != null) {
            for (IndexedField<?> field : resource.getIndexedFields()) {
                IndexField rtif = indexMap.get(field.getName());
                if (rtif == null) {
                    continue;
                }
                if (!rtif.isMultivalued()) {
                    for (Object value : field.getValues()) {

                        String fieldType = rtif.getType();
                        switch (fieldType) {
                            case "java.util.Date", "java.time.Instant" -> {
                                if (value instanceof Date date) {
                                    jsonObjectField.put(field.getName(), date.toInstant().toString());
                                } else if (value instanceof Instant instant) {
                                    jsonObjectField.put(field.getName(), instant.toString());
                                }

                            }
                            default -> jsonObjectField.put(field.getName(), value);
                        }
                    }
                } else {
                    List<Object> values = new ArrayList<>(field.getValues());
                    jsonObjectField.put(field.getName(), values);
                }
            }
        }
        // Build field-weighted semantic segments after payload extraction so future backends can reuse
        // the same TEXT/KEYWORD splitting policy independently of the embedding implementation.
        List<Segment> segments = ResourceEmbeddingSegmenter.segment(resource, new ArrayList<>(indexMap.values()));
        if (!segments.isEmpty()) {
            jsonObjectField.put("embedding", embeddingService.embed(segments));
        }
        return jsonObjectField;
    }

    private List<Map<String, Object>> createChunkEmbeddings(Resource resource) {
        List<IndexField> indexFields = new ArrayList<>(
                resourceTypeService.getResourceTypeIndexFields(resource.getResourceType().getName()));
        List<ResourceEmbeddingChunk> embeddingChunks = ResourceEmbeddingChunker.chunk(resource, indexFields);
        List<Map<String, Object>> chunks = new ArrayList<>();
        for (ResourceEmbeddingChunk chunk : embeddingChunks) {
            // Keep the chunk payload aligned with the PostgreSQL chunking/indexing pipeline so the
            // experimental ES representation can be compared against the SQL-backed search behavior.
            float[] embedding = embeddingService.embed(chunk.embeddingText());
            if (embedding != null && embedding.length == VECTOR_SIZE) {
                chunks.add(Map.of(
                        "field", chunk.fieldName(),
                        "value_ordinal", chunk.valueOrdinal(),
                        "field_chunk_idx", chunk.fieldChunkIdx(),
                        "content", chunk.content(),
                        "embedding", embedding
                ));
            }
        }
        return chunks;
    }
}
