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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.HttpFirewall;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the Spring wiring around {@link EncodedSlashHttpFirewallConfiguration}, complementing
 * {@link EncodedSlashHttpFirewallTest} (which only exercises the {@link HttpFirewall}
 * implementation directly).
 * <p>
 * This guards against the class-level {@code @ConditionalOnClass} on
 * {@link EncodedSlashHttpFirewallConfiguration} being weakened or removed. It does <b>not</b>,
 * and cannot, reproduce the original {@code model-service} regression itself (a
 * {@code NoClassDefFoundError} thrown while Spring reflectively resolves every method signature
 * in a class at once, breaking unrelated beans in the process): {@link FilteredClassLoader} only
 * hides a class from Spring's own classloader-based presence checks (e.g.
 * {@code ClassUtils.isPresent}), not from raw JVM reflection ({@code Class.getDeclaredMethods()})
 * on a class that's already loaded via this module's real classloader — and
 * {@code registry-starter-service} genuinely has {@code spring-security-web} on its own test
 * classpath. Reproducing the actual failure requires a consumer that truly lacks
 * {@code spring-security-web}, which is exactly what {@code model-service}'s
 * {@code ModelServiceApplicationTests.contextLoads} exercises end-to-end — that test is the real
 * guard against this regression class; this file only covers this class's own conditional
 * behavior in isolation.
 */
class EncodedSlashHttpFirewallConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EncodedSlashHttpFirewallConfiguration.class);

    @Test
    void beanRegisters_whenPropertyTrue_andSecurityWebOnClasspath() {
        contextRunner
                .withPropertyValues(
                        "registry.rest.generic-controller.allow-encoded-slash=true",
                        "registry.rest.generic-controller.encoded-slash-paths=service,datasource")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(WebSecurityCustomizer.class);
                });
    }

    @Test
    void beanAbsent_whenPropertyMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WebSecurityCustomizer.class);
        });
    }

    @Test
    void beanAbsent_whenPropertyExplicitlyFalse() {
        contextRunner
                .withPropertyValues("registry.rest.generic-controller.allow-encoded-slash=false")
                .run(context -> assertThat(context).doesNotHaveBean(WebSecurityCustomizer.class));
    }

    /**
     * {@link HttpFirewall} and {@link WebSecurityCustomizer} are hidden via
     * {@link FilteredClassLoader}, and the property is set so the condition would otherwise want
     * to register the bean — the context must still load cleanly, because the class-level
     * {@code @ConditionalOnClass} on {@link EncodedSlashHttpFirewallConfiguration} lets Spring
     * skip the class via its class-presence check before ever registering a bean definition for
     * it. See the class javadoc for why this doesn't reproduce the original whole-class-breakage
     * failure mode itself.
     */
    @Test
    void contextDoesNotFail_whenSpringSecurityWebIsAbsent_evenWithPropertyTrue() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(HttpFirewall.class, WebSecurityCustomizer.class))
                .withPropertyValues(
                        "registry.rest.generic-controller.allow-encoded-slash=true",
                        "registry.rest.generic-controller.encoded-slash-paths=service")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WebSecurityCustomizer.class);
                });
    }
}
