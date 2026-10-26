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

package gr.uoa.di.madgik.registry.rest;

import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.controllers.DumpController;
import gr.uoa.di.madgik.registry.controllers.ResourceController;
import gr.uoa.di.madgik.registry.controllers.ResourceTypeController;
import gr.uoa.di.madgik.registry.controllers.RestoreController;
import gr.uoa.di.madgik.registry.controllers.TypedResourceController;
import gr.uoa.di.madgik.registry.controllers.advice.GlobalExceptionHandler;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.testsupport.TypedResourceControllerTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TypedResourceControllerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=test", "registry.rest.enabled=false"})
class RestControllersDisabledIntegrationTest extends PostgreSqlTestContainerSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @Test
    void controllersAreNotRegisteredButServicesAndExceptionHandlerAre() {
        assertThat(context.getBeanNamesForType(ResourceController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ResourceTypeController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DumpController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(RestoreController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(TypedResourceController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ResourceService.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(GlobalExceptionHandler.class)).hasSize(1);
    }

    @Test
    void registryEndpointsAreNotServed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/resourceType/"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
    }
}
