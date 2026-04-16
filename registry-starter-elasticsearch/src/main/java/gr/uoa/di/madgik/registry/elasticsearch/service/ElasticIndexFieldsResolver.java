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

package gr.uoa.di.madgik.registry.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Resolves the text fields of an Elasticsearch index by inspecting its mapping.
 *
 * <p>Results are cached in {@value #CACHE_NAME} (keyed by index name) so that the
 * {@code getMapping} round-trip to Elasticsearch is only made once per index after startup.
 * Entries are evicted whenever the corresponding resource type is updated or deleted.</p>
 */
public class ElasticIndexFieldsResolver {

    static final String CACHE_NAME = "esTextFields";

    private static final Logger logger = LoggerFactory.getLogger(ElasticIndexFieldsResolver.class);

    private final ElasticsearchClient client;

    public ElasticIndexFieldsResolver(ElasticsearchClient client) {
        this.client = client;
    }

    /**
     * Returns the text-typed field paths for the given index.
     * The result is cached; the first call per index name triggers a live {@code getMapping} request.
     *
     * @param indexName the Elasticsearch index / resource type name
     * @return list of dotted field paths whose mapping type is {@code text}, never {@code null}
     */
    @Cacheable(value = CACHE_NAME, key = "#indexName")
    public List<String> getTextFields(String indexName) {
        try {
            GetMappingResponse mappingResponse = client.indices().getMapping(r -> r.index(indexName));
            IndexMappingRecord record = mappingResponse.mappings().values().stream().findFirst().orElse(null);
            if (record == null) {
                return Collections.emptyList();
            }
            return findTextFields(record.mappings().properties(), "");
        } catch (IOException e) {
            logger.warn("Reading resourceType '{}' fields from Elastic failed, using 'searchableArea' fallback.", indexName, e);
            return List.of("searchableArea");
        }
    }

    /**
     * Evicts the cached text-field list for the given index name.
     * Call this whenever the index mapping may have changed (resource type updated or deleted).
     *
     * @param indexName the Elasticsearch index / resource type name whose cache entry to remove
     */
    @CacheEvict(value = CACHE_NAME, key = "#indexName")
    public void evict(String indexName) {
    }

    private List<String> findTextFields(Map<String, Property> properties, String pathPrefix) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            String fullPath = pathPrefix.isEmpty() ? entry.getKey() : pathPrefix + "." + entry.getKey();
            Property prop = entry.getValue();
            if (prop.isText()) {
                result.add(fullPath);
            } else if (prop.isKeyword() && prop.keyword().fields() != null) {
                prop.keyword().fields().forEach((subName, subProp) -> {
                    if (subProp.isText()) result.add(fullPath + "." + subName);
                });
            } else if (prop.isObject() && prop.object().properties() != null) {
                result.addAll(findTextFields(prop.object().properties(), fullPath));
            } else if (prop.isNested() && prop.nested().properties() != null) {
                result.addAll(findTextFields(prop.nested().properties(), fullPath));
            }
        }
        return result;
    }
}
