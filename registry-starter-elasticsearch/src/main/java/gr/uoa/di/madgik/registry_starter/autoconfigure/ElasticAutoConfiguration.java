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

package gr.uoa.di.madgik.registry_starter.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.elasticsearch.IndexDbSync;
import gr.uoa.di.madgik.registry.elasticsearch.listeners.ElasticResourceListener;
import gr.uoa.di.madgik.registry.elasticsearch.listeners.ElasticResourceTypeListener;
import gr.uoa.di.madgik.registry.elasticsearch.service.ElasticOperationsService;
import gr.uoa.di.madgik.registry.elasticsearch.service.ElasticSearchService;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import gr.uoa.di.madgik.registry.monitor.ResourceTypeListener;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Auto-configuration entry point for the registry's Elasticsearch integration.
 *
 * <p>When enabled, this configuration provides the low-level Elasticsearch {@link RestClient},
 * wires the indexing and search service implementations, and registers listeners that keep the
 * search index synchronized with resource and resource-type lifecycle events.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(
        value="registry.elasticsearch.enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableRetry
@Import(IndexDbSync.class)
public class ElasticAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticAutoConfiguration.class);

    public ElasticAutoConfiguration() {
        logger.info("Elastic Autoconfiguration enabled");
    }

    @Bean
    @Primary
    @ConfigurationProperties("registry.elasticsearch")
    ElasticsearchProperties elasticsearchProperties() {
        return new ElasticsearchProperties();
    }

    /**
     * Creates the shared Elasticsearch low-level REST client from Spring Boot's bound properties.
     */
    @Bean
    RestClient restClient(ElasticsearchProperties properties) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (properties.getUsername() != null) {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
        }
        String host = properties.getUris().get(0);
        if (!host.startsWith("http")) { // add http prefix if missing (needed in the code below)
            host = "http://" + host;
        }
        String[] parts = host.split(":(//)?"); // split url to scheme / server / port
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(parts[1], Integer.parseInt(parts[2]), parts[0]))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        logger.info("Elasticsearch RestClient created for {}", host);
        return restClientBuilder.build();
    }

    /**
     * Creates a {@link JacksonJsonpMapper} that wraps the application's {@link ObjectMapper}.
     */
    @Bean
    JacksonJsonpMapper jacksonJsonpMapper(ObjectMapper objectMapper) {
        return new JacksonJsonpMapper(objectMapper);
    }

    /**
     * Creates the {@link ElasticsearchTransport} backed by the low-level REST client.
     */
    @Bean
    ElasticsearchTransport elasticsearchTransport(RestClient restClient, JacksonJsonpMapper jsonpMapper) {
        return new RestClientTransport(restClient, jsonpMapper);
    }

    /**
     * Creates the typed {@link ElasticsearchClient} used by all service beans.
     */
    @Bean
    ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    /**
     * Registers the Elasticsearch-backed index operations service as the primary implementation.
     */
    @Bean
    @Primary
    IndexOperationsService indexOperationsService(ResourceTypeService resourceTypeService,
                                                  ElasticsearchClient client,
                                                  EmbeddingService embeddingService,
                                                  ObjectMapper objectMapper) {
        return new ElasticOperationsService(resourceTypeService, client, embeddingService, objectMapper);
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
                                       ResourceTypeService resourceTypeService) {
        return new ElasticSearchService(client, jsonpMapper, embeddingService, resourceTypeService);
    }
}
