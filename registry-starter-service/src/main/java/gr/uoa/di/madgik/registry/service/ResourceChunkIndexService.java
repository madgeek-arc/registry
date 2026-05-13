/*
 * Copyright 2026-2026 OpenAIRE AMKE
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
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceChunk;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunk;
import gr.uoa.di.madgik.registry.domain.ResourceEmbeddingChunker;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.registry.service.EmbeddingService.VECTOR_SIZE;

@Service
public class ResourceChunkIndexService implements ResourceListener {

    private final ResourceChunkDao resourceChunkDao;
    private final EmbeddingService embeddingService;
    private final ResourceTypeService resourceTypeService;

    ResourceChunkIndexService(ResourceChunkDao resourceChunkDao,
                              EmbeddingService embeddingService,
                              ResourceTypeService resourceTypeService) {
        this.resourceChunkDao = resourceChunkDao;
        this.embeddingService = embeddingService;
        this.resourceTypeService = resourceTypeService;
    }

    @Override
    @Transactional
    public void resourceAdded(Resource resource) {
        reindex(resource);
    }

    @Override
    @Transactional
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        reindex(newResource);
    }

    @Override
    @Transactional
    public void resourceChangedType(Resource previousResource, Resource newResource, ResourceType previousResourceType, ResourceType resourceType) {
        reindex(newResource);
    }

    @Override
    @Transactional
    public void resourceDeleted(Resource resource) {
        if (resource != null && resource.getId() != null) {
            resourceChunkDao.deleteByResourceId(resource.getId());
        }
    }

    @Transactional
    public void reindex(Resource resource) {
        if (resource == null || resource.getId() == null) {
            return;
        }
        List<IndexField> indexFields = new ArrayList<>(
                resourceTypeService.getResourceTypeIndexFields(resource.getResourceType().getName()));
        List<ResourceEmbeddingChunk> chunks = ResourceEmbeddingChunker.chunk(resource, indexFields);
        String modelName = embeddingService.modelName();
        List<ResourceChunk> chunkEntities = new ArrayList<>();
        for (ResourceEmbeddingChunk embeddingChunk : chunks) {
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
