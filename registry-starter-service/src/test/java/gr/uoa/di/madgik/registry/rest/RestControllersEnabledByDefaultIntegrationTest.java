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
import gr.uoa.di.madgik.registry.controllers.TypedResourceController;
import gr.uoa.di.madgik.registry.controllers.advice.GlobalExceptionHandler;
import gr.uoa.di.madgik.registry.testsupport.TypedResourceControllerTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TypedResourceControllerTestApplication.class,
        properties = "spring.profiles.active=test")
class RestControllersEnabledByDefaultIntegrationTest extends PostgreSqlTestContainerSupport {

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @Test
    void controllersAreRegisteredOnce() {
        assertThat(context.getBeanNamesForType(ResourceController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(ResourceTypeController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(DumpController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(TypedResourceController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(GlobalExceptionHandler.class)).hasSize(1);
    }
}
