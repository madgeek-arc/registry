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

import gr.uoa.di.madgik.registry.controllers.GenericController;
import gr.uoa.di.madgik.registry.configuration.BackupRestoreConfig;
import gr.uoa.di.madgik.registry.configuration.BatchConfig;
import gr.uoa.di.madgik.registry.configuration.HibernateConfiguration;
import gr.uoa.di.madgik.registry.configuration.ServiceConfiguration;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.startup.ResourceTypeInit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;

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
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAsyncCacheMode(false);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterAccess(1, TimeUnit.HOURS));

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
