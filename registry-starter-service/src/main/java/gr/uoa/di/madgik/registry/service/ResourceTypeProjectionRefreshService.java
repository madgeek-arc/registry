/*
 * Copyright 2026-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.dao.ResourceChunkDao;
import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceChunk;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunk;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunker;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.index.IndexMapper;
import gr.uoa.di.madgik.registry.index.IndexMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.registry.service.EmbeddingService.VECTOR_SIZE;

@Service
public class ResourceTypeProjectionRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeProjectionRefreshService.class);

    private final ResourceDao resourceDao;
    private final ResourceChunkDao resourceChunkDao;
    private final IndexMapperFactory indexMapperFactory;
    private final EmbeddingService embeddingService;

    public ResourceTypeProjectionRefreshService(ResourceDao resourceDao,
                                                ResourceChunkDao resourceChunkDao,
                                                IndexMapperFactory indexMapperFactory,
                                                EmbeddingService embeddingService) {
        this.resourceDao = resourceDao;
        this.resourceChunkDao = resourceChunkDao;
        this.indexMapperFactory = indexMapperFactory;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public void refresh(ResourceType resourceType) {
        List<Resource> resources = resourceDao.getResource(resourceType);
        if (resources.isEmpty()) {
            return;
        }

        IndexMapper indexMapper = createIndexMapper(resourceType);
        List<IndexField> indexFields = resourceType.getIndexFields();
        logger.info("Refreshing projections for {} resources of type '{}'", resources.size(), resourceType.getName());

        for (Resource resource : resources) {
            refreshIndexedFields(resource, resourceType, indexMapper);
            resourceDao.mergeResource(resource);
            refreshChunks(resource, indexFields);
        }
    }

    private IndexMapper createIndexMapper(ResourceType resourceType) {
        try {
            return indexMapperFactory.createIndexMapper(resourceType);
        } catch (Exception e) {
            throw new ServiceException("Failed to create index mapper for resource type " + resourceType.getName(), e);
        }
    }

    private void refreshIndexedFields(Resource resource, ResourceType resourceType, IndexMapper indexMapper) {
        List<IndexedField> refreshedFields;
        try {
            refreshedFields = indexMapper.getValues(resource.getPayload(), resourceType);
        } catch (Exception e) {
            throw new ServiceException("Failed to refresh indexed fields for resource " + resource.getId(), e);
        }

        for (IndexedField indexedField : refreshedFields) {
            indexedField.setResource(resource);
        }

        resource.setResourceType(resourceType);
        resource.setResourceTypeName(resourceType.getName());
        if (resource.getIndexedFields() == null) {
            resource.setIndexedFields(new ArrayList<>());
        } else {
            resource.getIndexedFields().clear();
        }
        resource.getIndexedFields().addAll(refreshedFields);
    }

    private void refreshChunks(Resource resource, List<IndexField> indexFields) {
        List<ResourceEmbeddingChunk> embeddingChunks = ResourceEmbeddingChunker.chunk(resource, indexFields);
        String modelName = embeddingService.modelName();
        List<ResourceChunk> chunkEntities = new ArrayList<>();
        for (ResourceEmbeddingChunk embeddingChunk : embeddingChunks) {
            float[] embedding = embeddingService.embed(embeddingChunk.embeddingText());
            if (embedding == null || embedding.length != VECTOR_SIZE) {
                continue;
            }
            ResourceChunk chunk = new ResourceChunk();
            chunk.setResourceId(resource.getId());
            chunk.setChunkIdx(embeddingChunk.chunkIdx());
            chunk.setFieldName(embeddingChunk.fieldName());
            chunk.setValueOrdinal(embeddingChunk.valueOrdinal());
            chunk.setFieldChunkIdx(embeddingChunk.fieldChunkIdx());
            chunk.setContent(embeddingChunk.content());
            chunk.setEmbedding(embedding);
            chunk.setEmbeddingModel(modelName);
            chunkEntities.add(chunk);
        }
        resourceChunkDao.replaceChunks(resource.getId(), chunkEntities);
    }
}
