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

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all stereotype beans contributed by the registry service starter.
 *
 * <p>This is a plain {@code @Configuration} class — not an {@code @AutoConfiguration} — and is
 * imported explicitly by {@link RegistryServiceAutoConfiguration}. Keeping the component scan
 * here (rather than on the auto-configuration itself) is the Boot 4 convention: auto-configuration
 * entry points must not carry {@code @ComponentScan}.
 *
 * <p>The scan of {@code gr.uoa.di.madgik.registry.service} intentionally spans two JARs on the
 * classpath ({@code registry-starter-service} and {@code registry-core-helper}), picking up
 * {@code FacetLabelService}, {@code GenericResourceManager}, and {@code ParserPool} from the
 * helper module alongside the service implementations in this module.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan({
        "gr.uoa.di.madgik.registry.service",      // @Service impls + registry-core-helper beans (cross-jar, same package)
        "gr.uoa.di.madgik.registry.dao",           // @Repository impls
        "gr.uoa.di.madgik.registry.index",         // @Component index/mapper/factory beans
        "gr.uoa.di.madgik.registry.monitor",       // @Component @Aspect monitors and listeners
        "gr.uoa.di.madgik.registry.validation",    // @Service ResourceSchemaValidator
        "gr.uoa.di.madgik.registry.backup",        // @Service + @StepScope batch components (recursive — covers .dump and .restore)
        "gr.uoa.di.madgik.registry.controllers",   // @RestController controllers (from registry-core-rest jar)
})
class RegistryServiceComponentsConfiguration {}
