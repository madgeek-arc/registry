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

import gr.uoa.di.madgik.registry.domain.Segment;
import gr.uoa.di.madgik.registry.service.EmbeddingService;
import gr.uoa.di.madgik.registry.service.WeightedSegmentEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.ai.model.transformers.autoconfigure.TransformersEmbeddingModelAutoConfiguration"
})
public class RegistryEmbeddingServiceAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RegistryEmbeddingServiceAutoConfiguration.class);

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    @ConditionalOnMissingBean(EmbeddingService.class)
    EmbeddingService weightingEmbeddingService(EmbeddingModel embeddingModel, Environment environment) {
        return new WeightedSegmentEmbeddingService(embeddingModel, resolveEmbeddingModelName(environment, embeddingModel));
    }

    @Bean
    @ConditionalOnMissingBean(value = {EmbeddingService.class, EmbeddingModel.class})
    EmbeddingService noopEmbeddingService() {
        logger.warn("No EmbeddingService found. Using noopEmbeddingService()");
        return new EmbeddingService() {
            @Override
            public float[] embed(String text) {
                return new float[0];
            }

            @Override
            public float[] embed(List<Segment> segments) {
                return new float[0];
            }

            @Override
            public String modelName() {
                return "noop";
            }
        };
    }

    private String resolveEmbeddingModelName(Environment environment, EmbeddingModel embeddingModel) {
        String[] candidateProperties = {
                "spring.ai.embedding.transformer.onnx.modelUri",
                "spring.ai.openai.embedding.options.model",
                "spring.ai.ollama.embedding.options.model",
                "spring.ai.vertex.ai.embedding.text.options.model",
                "spring.ai.bedrock.aws.embedding.cohere.options.model",
                "spring.ai.bedrock.aws.embedding.titan.options.model",
                "spring.ai.azure.openai.embedding.options.deployment-name",
                "spring.ai.mistral.ai.embedding.options.model",
                "spring.ai.transformers.embedding.model"
        };

        for (String property : candidateProperties) {
            String value = environment.getProperty(property);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return embeddingModel.getClass().getName();
    }
}
