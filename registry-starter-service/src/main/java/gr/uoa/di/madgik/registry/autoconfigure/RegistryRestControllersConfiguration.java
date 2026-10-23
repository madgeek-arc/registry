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

import gr.uoa.di.madgik.registry.controllers.TypedResourceController;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Registers the registry REST controllers (resources, resource types, schemas, search, versions,
 * ResourceSync, dump/restore and {@link TypedResourceController}).
 *
 * <p>The registry adds no authentication or authorisation of its own: the application that registers these
 * controllers is responsible for guarding their paths. Applications that don't want the endpoints exposed
 * set {@code registry.rest.enabled=false}; the registry services remain available as beans. The exception
 * handler in {@code controllers.advice} is not affected.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "registry.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "gr.uoa.di.madgik.registry.controllers", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TypedResourceController.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "gr\\.uoa\\.di\\.madgik\\.registry\\.controllers\\.advice\\..*")
})
class RegistryRestControllersConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RegistryRestControllersConfiguration.class);

    RegistryRestControllersConfiguration() {
        logger.info("Registry REST controllers registered without authentication. Guard their paths in the "
                + "application's security configuration, or set registry.rest.enabled=false to leave them out");
    }

    @Bean
    @ConditionalOnBean(GenericResourceService.class)
    @ConditionalOnMissingBean(TypedResourceController.class)
    @ConditionalOnProperty(prefix = "registry.rest.typed-resources-controller", name = "enabled", havingValue = "true", matchIfMissing = true)
    TypedResourceController typedResourceController(GenericResourceService genericResourceService, VersionService versionService) {
        return new TypedResourceController(genericResourceService, versionService);
    }
}
