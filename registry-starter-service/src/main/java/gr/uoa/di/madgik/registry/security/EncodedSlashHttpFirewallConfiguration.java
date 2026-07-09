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

package gr.uoa.di.madgik.registry.security;

import gr.uoa.di.madgik.registry.controllers.GenericController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.HttpFirewall;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the {@code encodedSlashHttpFirewallCustomizer} bean in its own class, gated by a
 * class-level {@link ConditionalOnClass}. Spring Security's web module (which provides
 * {@link HttpFirewall} and {@link WebSecurityCustomizer}) is optional for consumers of this
 * starter. If this bean lived directly in {@code RegistryServiceAutoConfiguration}, reflective
 * introspection of that class (needed to evaluate conditions on its other, unrelated beans)
 * would try to resolve every method signature at once, including this one — and throw
 * {@code NoClassDefFoundError} for consumers without {@code spring-security-web} on the
 * classpath, breaking the whole auto-configuration class. Isolating it here means the class is
 * only introspected when the class-level condition already holds.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({HttpFirewall.class, WebSecurityCustomizer.class})
public class EncodedSlashHttpFirewallConfiguration {

    /**
     * Off by default, same flag as {@code tomcatEncodedSlashCustomizer}. Relaxes the firewall
     * only for the configured path prefixes plus {@code GenericController.BASE_PATH} ("records"),
     * which is always included since {@code GenericController} is always registered by this
     * starter — every other path keeps Spring Security's default strict firewall. A consumer
     * only needs to list its own extra paths, e.g. "service,datasource"; "records" doesn't need
     * to be repeated. Named (not type-based) {@code ConditionalOnMissingBean} so a consumer can
     * supply its own {@code WebSecurityCustomizer} under a different name without conflict; note
     * Spring Security accepts a {@code List<WebSecurityCustomizer>}, so if a consumer's own
     * customizer also calls {@code .httpFirewall(...)}, the last one applied wins.
     */
    @Bean
    @ConditionalOnMissingBean(name = "encodedSlashHttpFirewallCustomizer")
    @ConditionalOnProperty(prefix = "registry.rest.generic-controller", name = "allow-encoded-slash", havingValue = "true")
    WebSecurityCustomizer encodedSlashHttpFirewallCustomizer(
            @Value("${registry.rest.generic-controller.encoded-slash-paths:}")
            List<String> encodedSlashPaths) {
        Set<String> paths = new LinkedHashSet<>(encodedSlashPaths);
        paths.add(GenericController.BASE_PATH);
        HttpFirewall firewall = new EncodedSlashHttpFirewall(paths);
        return web -> web.httpFirewall(firewall);
    }
}
