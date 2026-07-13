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

package gr.uoa.di.madgik.registry.testsupport;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal app used only to trigger real {@code @EnableAutoConfiguration} discovery
 * (picking up {@code RegistryServiceAutoConfiguration} from
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * for embedded-server tests that need the full production wiring, unlike the narrower
 * {@code DatabaseConfiguration} test config used by DAO/service-level tests.
 *
 * <p>Deliberately lives outside {@code gr.uoa.di.madgik.registry.controllers}:
 * {@code @SpringBootApplication}'s implicit component scan would otherwise re-scan
 * {@code TypedResourceController} as a plain {@code @RestController} stereotype bean, bypassing the
 * exclude-filter that {@code RegistryServiceComponentsConfiguration} relies on to let the
 * conditional {@code typedResourceController()} @Bean method be the sole source of that bean.
 *
 * <p>Shared across every embedded-server test package that needs this wiring (encoded-slash
 * firewall tests, general {@code TypedResourceController} CRUD tests, etc.) rather than duplicated
 * per package.
 */
@SpringBootApplication
public class TypedResourceControllerTestApplication {

    /**
     * Most of these tests are about behavior that runs ahead of authorization in the filter
     * chain (e.g. the HttpFirewall), or don't exercise security at all — without this, Spring
     * Security's default auto-configured chain would secure every endpoint and requests would
     * fail with 401 before ever reaching the thing being tested.
     */
    @Bean
    SecurityFilterChain permitAllFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }
}
