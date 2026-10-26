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

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Points Spring AI's transformers embedding model at the {@code all-MiniLM-L6-v2} ONNX model and tokenizer
 * packaged in this jar, so no model is downloaded at runtime.
 *
 * <p>The values are added as the lowest-precedence property source: any value set by the application
 * (configuration files, environment variables, system properties) takes priority.
 */
public class RegistryEmbeddingModelDefaults implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "registryEmbeddingModelDefaults";

    static final Map<String, Object> DEFAULTS = Map.of(
            "spring.ai.model.embedding", "transformers",
            "spring.ai.embedding.transformer.onnx.modelUri", "classpath:/onnx/all-MiniLM-L6-v2/model.onnx",
            "spring.ai.embedding.transformer.onnx.modelOutputName", "token_embeddings",
            "spring.ai.embedding.transformer.tokenizer.uri", "classpath:/onnx/all-MiniLM-L6-v2/tokenizer.json");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
        }
    }
}
