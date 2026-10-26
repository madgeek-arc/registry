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

import com.sun.net.httpserver.HttpServer;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ResourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryClientAutoConfigurationTest {

    private HttpServer server;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private volatile long responseDelayMillis;

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            try {
                Thread.sleep(responseDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{\"id\":\"r1\",\"payload\":\"{}\",\"payloadFormat\":\"json\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RegistryClientAutoConfiguration.class))
                .withPropertyValues("registry.base=http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void timeoutsDefaultToTenAndSixtySeconds() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RegistryClientProperties properties = context.getBean(RegistryClientProperties.class);
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
        });
    }

    @Test
    void resourceServiceCallsConfiguredRegistryThroughSharedRestTemplate() {
        contextRunner.run(context -> {
            Resource resource = context.getBean(ResourceService.class).getResource("r1");

            assertThat(resource.getId()).isEqualTo("r1");
            assertThat(lastPath.get()).isEqualTo("/resources/whatever/r1");
        });
    }

    @Test
    void slowRegistryFailsAfterReadTimeout() {
        responseDelayMillis = 2_000;
        contextRunner
                .withPropertyValues("registry.client.read-timeout=200ms")
                .run(context -> assertThatThrownBy(() -> context.getBean(ResourceService.class).getResource("r1"))
                        .isInstanceOf(ResourceAccessException.class));
    }

    @Test
    void applicationRestTemplateTakesPrecedence() {
        RestTemplate custom = new RestTemplate();
        contextRunner
                .withBean(RestTemplate.class, () -> custom)
                .run(context -> assertThat(context.getBean(RestTemplate.class)).isSameAs(custom));
    }
}
