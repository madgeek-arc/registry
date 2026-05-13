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

import java.util.List;

public interface EmbeddingService {

    /** Must match the output dimension of the configured embedding model. */
    public static int VECTOR_SIZE = 384;

    /**
     * Creates an embedding vector of the provided text.
     *
     * @param text the text to create the embedding for
     * @return the embedding vector
     */
    float[] embed(String text);

    /**
     * Creates an embedding vector based on the provided information.
     *
     * @param segments a list of attributes, values and weights which will be used to create an embedding
     * @return the embedding vector
     */
    float[] embed(List<Segment> segments);

    default String modelName() {
        return getClass().getSimpleName();
    }
}
