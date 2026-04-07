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

package gr.uoa.di.madgik.registry.autoconfigure;

import gr.uoa.di.madgik.registry.ResourceTypeInit;
import gr.uoa.di.madgik.registry.controllers.GenericController;
import gr.uoa.di.madgik.registry.configuration.BackupRestoreConfig;
import gr.uoa.di.madgik.registry.configuration.BatchConfig;
import gr.uoa.di.madgik.registry.configuration.HibernateConfiguration;
import gr.uoa.di.madgik.registry.configuration.ServiceConfiguration;
import gr.uoa.di.madgik.registry.domain.Segment;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

@AutoConfiguration
@EnableCaching
@Import({
        HibernateConfiguration.class,
        BatchConfig.class,
        BackupRestoreConfig.class,
        ServiceConfiguration.class,
        ResourceTypeInit.class,
        RegistryServiceComponentsConfiguration.class,
})
public class RegistryServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    EmbeddingService noopEmbeddingService() {
        return new EmbeddingService() {
            @Override public float[] embed(String text) { return new float[0]; }
            @Override public float[] embed(List<Segment> segments) { return new float[0]; }
        };
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAsyncCacheMode(false);

        return manager;
    }

    @Bean
    @ConditionalOnBean(GenericResourceService.class)
    @ConditionalOnMissingBean(GenericController.class)
    @ConditionalOnProperty(prefix = "registry.rest.generic-controller", name = "enabled", havingValue = "true", matchIfMissing = true)
    GenericController genericController(GenericResourceService genericResourceService) {
        return new GenericController(genericResourceService);
    }

}
