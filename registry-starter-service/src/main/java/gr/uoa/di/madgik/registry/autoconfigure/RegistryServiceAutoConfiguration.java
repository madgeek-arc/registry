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
import gr.uoa.di.madgik.registry.security.EncodedSlashHttpFirewall;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.startup.ResourceTypeInit;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.HttpFirewall;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    /**
     * Off by default: only consumers whose domain primary keys can contain "/" (and thus
     * reach GenericController's {@code {resourceType}/{id}/...} routes with a URL-encoded
     * slash in {@code {id}}) need this. Everyone else keeps Tomcat's default (safer) handling,
     * which rejects encoded slashes outright. "passthrough" (not "decode") is required: it
     * leaves "%2F" encoded in the raw request URI, so Spring's PathPatternParser can percent-decode
     * it within the single matched {@code {id}} segment instead of Tomcat splitting it into an
     * extra path segment.
     */
    @Bean
    @ConditionalOnClass(Connector.class)
    @ConditionalOnMissingBean(name = "tomcatEncodedSlashCustomizer")
    @ConditionalOnProperty(prefix = "registry.rest.generic-controller", name = "allow-encoded-slash", havingValue = "true")
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatEncodedSlashCustomizer() {
        return factory -> factory.addConnectorCustomizers(
                connector -> connector.setEncodedSolidusHandling("passthrough"));
    }

    /**
     * Off by default, same flag as tomcatEncodedSlashCustomizer. Relaxes the firewall only for
     * the configured path prefixes plus "records" (GenericController's base path), which is
     * always included since GenericController is always registered by this starter — every
     * other path keeps Spring Security's default strict firewall. A consumer only needs to list
     * its own extra paths, e.g. "service,datasource"; "records" doesn't need to be repeated.
     * Only takes effect when the consumer has Spring Security on its classpath
     * (ConditionalOnClass) — consumers without Spring Security have no firewall to relax and get
     * only the Tomcat-level fix above, which is all they need. Named (not type-based)
     * ConditionalOnMissingBean so a consumer can supply its own WebSecurityCustomizer under a
     * different name without conflict; note Spring Security accepts a
     * List<WebSecurityCustomizer>, so if a consumer's own customizer also calls
     * .httpFirewall(...), the last one applied wins.
     */
    @Bean
    @ConditionalOnClass({HttpFirewall.class, WebSecurityCustomizer.class})
    @ConditionalOnMissingBean(name = "encodedSlashHttpFirewallCustomizer")
    @ConditionalOnProperty(prefix = "registry.rest.generic-controller", name = "allow-encoded-slash", havingValue = "true")
    WebSecurityCustomizer encodedSlashHttpFirewallCustomizer(
            @Value("${registry.rest.generic-controller.encoded-slash-paths:}")
            List<String> encodedSlashPaths) {
        Set<String> paths = new LinkedHashSet<>(encodedSlashPaths);
        paths.add("records");
        HttpFirewall firewall = new EncodedSlashHttpFirewall(paths);
        return web -> web.httpFirewall(firewall);
    }

}
