/*
 * Copyright 2026-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

public class WeightingEmbeddingService implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(WeightingEmbeddingService.class);
    private final EmbeddingModel embeddingModel;
    private final String modelName;

    public WeightingEmbeddingService(EmbeddingModel embeddingModel, String modelName) {
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * Creates an embedding vector based on the provided information.
     * Generates a vector for each of the {@code segments} and synthesizes them using their weights to create the
     * embedding vector.
     *
     * @param segments a list of attributes, values and weights which will be used to create an embedding
     * @return the embedding vector - not l2 normalized
     */
    @Override
    public float[] embed(List<Segment> segments) {
        float weightSum = 0; // to normalize at the end
        float[] result = new float[VECTOR_SIZE];
        if (segments == null || segments.isEmpty()) {
            throw new RuntimeException("No text has been provided to create an embedding vector.");
        }
        for (Segment segment : segments) {
            if (segment.getWeight() > 0 && !segment.getValues().isEmpty()) {
                weightSum += segment.getWeight();
                float[] pooledVector = new float[VECTOR_SIZE];
                for (String text : segment.getValues()) {
                    String embeddingText = "[%s]: %s".formatted(segment.getLabel(), text);
                    float[] embedding = embeddingModel.embed(embeddingText);
                    for (int i = 0; i < VECTOR_SIZE; i++) { // adds weighted embedding to pool
                        pooledVector[i] += (embedding[i] * segment.getWeight());
                    }
                }
                for (int i = 0; i < VECTOR_SIZE; i++) { // creates mean(pooledVector) and adds it to result
                    result[i] += (pooledVector[i] / segment.getValues().size());
                }
            }
        }
        for (int i = 0; i < VECTOR_SIZE; i++) { // scale down the values using the weightSum
            result[i] /= (weightSum > 0 ? weightSum : 1); // if weight
        }
        // It is possible to normalize the result and use dot product instead of cosine similarity.
        return result;
    }

    @Override
    public String modelName() {
        return modelName;
    }
}
