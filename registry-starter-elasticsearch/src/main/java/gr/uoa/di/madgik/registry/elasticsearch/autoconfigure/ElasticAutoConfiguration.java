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

package gr.uoa.di.madgik.registry.elasticsearch.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.elasticsearch.SearchIndexConsistencyService;
import gr.uoa.di.madgik.registry.elasticsearch.listeners.ElasticResourceListener;
import gr.uoa.di.madgik.registry.elasticsearch.listeners.ElasticResourceTypeListener;
import gr.uoa.di.madgik.registry.elasticsearch.service.ElasticOperationsService;
import gr.uoa.di.madgik.registry.elasticsearch.service.ElasticSearchService;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import gr.uoa.di.madgik.registry.monitor.ResourceTypeListener;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.annotation.EnableRetry;

import java.net.URI;
import java.util.List;

/**
 * Autoconfiguration entry point for the registry's Elasticsearch integration.
 *
 * <p>When enabled, this configuration wires the official Elasticsearch Java API client
 * ({@link ElasticsearchClient}), exposes the transport beans it depends on, and registers the
 * indexing/search services plus listeners that keep the search index synchronized with resource
 * and resource-type lifecycle events.</p>
 */
@AutoConfiguration(afterName = "gr.uoa.di.madgik.registry.autoconfigure.RegistryServiceAutoConfiguration")
@ConditionalOnProperty(
        value="registry.elasticsearch.enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableRetry
@EnableConfigurationProperties(RegistryElasticsearchProperties.class)
@Import({SearchIndexConsistencyService.class})
public class ElasticAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticAutoConfiguration.class);

    public ElasticAutoConfiguration() {
        logger.info("Elastic Autoconfiguration enabled");
    }

    /**
     * Creates the low-level REST transport client required by the official typed API client.
     */
    @Bean
    @ConditionalOnMissingBean
    Rest5Client restClient(RegistryElasticsearchProperties properties) {
        URI uri = firstUri(properties.getUris());
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (properties.getUsername() != null) {
            credentialsProvider.setCredentials(
                    new AuthScope(uri.getHost(), port(uri)),
                    new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword().toCharArray()));
        }
        Rest5ClientBuilder restClientBuilder = Rest5Client.builder(uri)
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        logger.info("Elasticsearch REST transport created for {}", uri);
        return restClientBuilder.build();
    }

    /**
     * Creates a {@link JacksonJsonpMapper} that wraps the application's {@link ObjectMapper}.
     */
    @Bean
    @ConditionalOnMissingBean
    JacksonJsonpMapper jacksonJsonpMapper(ObjectMapper objectMapper) {
        return new JacksonJsonpMapper(objectMapper);
    }

    /**
     * Creates the {@link ElasticsearchTransport} backed by the low-level REST client.
     */
    @Bean
    @ConditionalOnMissingBean
    ElasticsearchTransport elasticsearchTransport(Rest5Client restClient, JacksonJsonpMapper jsonpMapper) {
        return new Rest5ClientTransport(restClient, jsonpMapper);
    }

    /**
     * Creates the typed {@link ElasticsearchClient} used by all service beans.
     */
    @Bean
    @ConditionalOnMissingBean
    ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    /**
     * Registers the Elasticsearch-backed index operations service as the primary implementation.
     */
    @Bean
    @Primary
    IndexOperationsService indexOperationsService(ResourceTypeService resourceTypeService,
                                                  ResourceService resourceService,
                                                  ElasticsearchClient client,
                                                  EmbeddingService embeddingService,
                                                  ObjectMapper objectMapper) {
        return new ElasticOperationsService(resourceTypeService, resourceService, client, embeddingService, objectMapper);
    }

    /**
     * Registers the resource-type listener responsible for index creation and deletion.
     */
    @Bean
    ResourceTypeListener elasticResourceTypeListener(IndexOperationsService indexOperationsService) {
        return new ElasticResourceTypeListener(indexOperationsService);
    }

    /**
     * Registers the resource listener responsible for document-level index updates.
     */
    @Bean
    ResourceListener elasticResourceListener(IndexOperationsService indexOperationsService) {
        return new ElasticResourceListener(indexOperationsService);
    }

    /**
     * Registers the Elasticsearch-backed search service as the primary search implementation.
     */
    @Bean
    @Primary
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SearchService elasticSearchService(ElasticsearchClient client, JacksonJsonpMapper jsonpMapper,
                                       EmbeddingService embeddingService,
                                       ResourceTypeService resourceTypeService,
                                       RegistryElasticsearchProperties elasticsearchProperties) {
        return new ElasticSearchService(client, jsonpMapper, embeddingService, resourceTypeService,
                elasticsearchProperties);
    }

    private static URI firstUri(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            throw new IllegalStateException("registry.elasticsearch.uris must contain at least one URI");
        }
        String rawUri = uris.getFirst();
        if (!rawUri.contains("://")) {
            rawUri = "http://" + rawUri;
        }
        return URI.create(rawUri);
    }

    private static int port(URI uri) {
        return uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 9200) : uri.getPort();
    }
}
