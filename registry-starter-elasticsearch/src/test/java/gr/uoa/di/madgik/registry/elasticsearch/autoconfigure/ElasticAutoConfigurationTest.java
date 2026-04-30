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

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import gr.uoa.di.madgik.registry.elasticsearch.SearchIndexConsistencyService;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import gr.uoa.di.madgik.registry.monitor.ResourceTypeListener;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.IndexOperationsService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeProjectionService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElasticAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ElasticAutoConfiguration.class))
            .withPropertyValues(
                    "registry.elasticsearch.enabled=true",
                    "registry.elasticsearch.uris=http://localhost:9200",
                    "registry.elasticsearch.username=test",
                    "registry.elasticsearch.password=secret")
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().findAndAddModules().build())
            .withBean(ResourceTypeService.class, () -> {
                ResourceTypeService service = mock(ResourceTypeService.class);
                when(service.getAllResourceType()).thenReturn(List.of());
                return service;
            })
            .withBean(ResourceService.class, () -> mock(ResourceService.class))
            .withBean(ResourceTypeProjectionService.class, () -> mock(ResourceTypeProjectionService.class))
            .withBean(EmbeddingService.class, () -> mock(EmbeddingService.class))
            .withBean("registryDataSource", DataSource.class, () -> mock(DataSource.class))
            // Simulate the CacheManager normally provided by registry-starter-service.
            // The ES starter always runs alongside the service starter in production.
            .withBean(CacheManager.class, () -> new ConcurrentMapCacheManager());

    @Test
    void autoConfiguration_registers_elasticsearch_beans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RegistryElasticsearchProperties.class);
            assertThat(context).hasSingleBean(SearchIndexConsistencyService.class);
            assertThat(context).hasSingleBean(IndexOperationsService.class);
            assertThat(context).hasSingleBean(SearchService.class);
            assertThat(context).hasSingleBean(ResourceListener.class);
            assertThat(context).hasSingleBean(ResourceTypeListener.class);
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context).hasSingleBean(JsonpMapper.class);
            assertThat(context.getBean(JsonpMapper.class)).isInstanceOf(Jackson3JsonpMapper.class);
            assertThat(context).hasNotFailed();

            RegistryElasticsearchProperties properties = context.getBean(RegistryElasticsearchProperties.class);
            assertThat(properties.getUris()).containsExactly("http://localhost:9200");
            assertThat(properties.getUsername()).isEqualTo("test");
            assertThat(properties.getPassword()).isEqualTo("secret");
        });
    }

    @Test
    void autoConfiguration_is_disabled_when_property_is_false() {
        contextRunner.withPropertyValues("registry.elasticsearch.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IndexOperationsService.class);
                    assertThat(context).doesNotHaveBean(SearchService.class);
                    assertThat(context).doesNotHaveBean(SearchIndexConsistencyService.class);
                });
    }
}
