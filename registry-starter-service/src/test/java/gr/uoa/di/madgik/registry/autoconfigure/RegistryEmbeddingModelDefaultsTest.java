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

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryEmbeddingModelDefaultsTest {

    private final RegistryEmbeddingModelDefaults defaults = new RegistryEmbeddingModelDefaults();

    @Test
    void pointsSpringAiAtThePackagedModel() {
        StandardEnvironment environment = new StandardEnvironment();

        defaults.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.ai.model.embedding")).isEqualTo("transformers");
        String modelUri = environment.getProperty("spring.ai.embedding.transformer.onnx.modelUri");
        String tokenizerUri = environment.getProperty("spring.ai.embedding.transformer.tokenizer.uri");
        assertThat(new DefaultResourceLoader().getResource(modelUri).exists()).isTrue();
        assertThat(new DefaultResourceLoader().getResource(tokenizerUri).exists()).isTrue();
    }

    @Test
    void applicationValuesTakePrecedence() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MockPropertySource("application")
                .withProperty("spring.ai.embedding.transformer.onnx.modelUri", "file:/models/custom.onnx"));

        defaults.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.ai.embedding.transformer.onnx.modelUri")).isEqualTo("file:/models/custom.onnx");
        assertThat(environment.getProperty("spring.ai.embedding.transformer.tokenizer.uri"))
                .isEqualTo("classpath:/onnx/all-MiniLM-L6-v2/tokenizer.json");
    }

    @Test
    void isRegisteredAsEnvironmentPostProcessor() throws Exception {
        String factories = new String(getClass().getResourceAsStream("/META-INF/spring.factories").readAllBytes());

        assertThat(factories).contains(RegistryEmbeddingModelDefaults.class.getName());
    }
}
