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
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link ElasticIndexFieldsResolver} honours its {@code @Cacheable} and
 * {@code @CacheEvict} contracts when wired through a real Spring AOP proxy.
 *
 * <p>Plain {@code new ElasticIndexFieldsResolver(client)} bypasses the proxy, so this test
 * uses a minimal Spring context ({@link TestConfig}) to ensure the cache interceptor is active.</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ElasticIndexFieldsResolverCacheTest.TestConfig.class)
class ElasticIndexFieldsResolverCacheTest {

    @EnableCaching
    @Configuration
    static class TestConfig {

        // Shared mock instances exposed as static fields so tests can verify interactions
        // without needing to retrieve them from the context.
        static final ElasticsearchClient CLIENT = mock(ElasticsearchClient.class);
        static final ElasticsearchIndicesClient INDICES_CLIENT = mock(ElasticsearchIndicesClient.class);

        static {
            when(CLIENT.indices()).thenReturn(INDICES_CLIENT);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(ElasticIndexFieldsResolver.CACHE_NAME);
        }

        @Bean
        ElasticIndexFieldsResolver resolver() {
            return new ElasticIndexFieldsResolver(CLIENT);
        }
    }

    @Autowired
    ElasticIndexFieldsResolver resolver;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() throws IOException {
        // Reset mock invocation counts and re-stub before each test.
        reset(TestConfig.INDICES_CLIENT);
        doThrow(new IOException("mapping unavailable"))
                .when(TestConfig.INDICES_CLIENT)
                .getMapping(any(Function.class));
        // Clear the cache so each test starts cold.
        cacheManager.getCache(ElasticIndexFieldsResolver.CACHE_NAME).clear();
    }

    @Test
    void getTextFields_returnsEmptyListWhenMappingCallFails() {
        List<String> fields = resolver.getTextFields("provider");
        assertEquals(List.of(), fields);
    }

    @Test
    void getTextFields_secondCallReturnsCachedResultWithoutHittingElasticsearch() throws IOException {
        resolver.getTextFields("provider");
        resolver.getTextFields("provider");

        // getMapping must have been called exactly once — the second call hits the cache.
        verify(TestConfig.INDICES_CLIENT, times(1)).getMapping(any(Function.class));
    }

    @Test
    void getTextFields_differentIndexNamesAreCachedIndependently() throws IOException {
        resolver.getTextFields("provider");
        resolver.getTextFields("service");

        // Each distinct index name is a separate cache key — two mapping calls expected.
        verify(TestConfig.INDICES_CLIENT, times(2)).getMapping(any(Function.class));
    }

    @Test
    void evict_causesSubsequentCallToBypassCache() throws IOException {
        resolver.getTextFields("provider");
        resolver.evict("provider");
        resolver.getTextFields("provider");

        // After eviction the next call must go back to Elasticsearch.
        verify(TestConfig.INDICES_CLIENT, times(2)).getMapping(any(Function.class));
    }

    @Test
    void evict_doesNotAffectOtherCachedIndices() throws IOException {
        resolver.getTextFields("provider");
        resolver.getTextFields("service");

        resolver.evict("provider");          // evict only "provider"
        resolver.getTextFields("service");   // should still be served from cache

        // "service" was fetched once and evicted never → still one call for "service".
        // "provider" was fetched once, evicted, not re-fetched → total = 2.
        verify(TestConfig.INDICES_CLIENT, times(2)).getMapping(any(Function.class));
    }
}
