/*
 * Copyright 2026-2026 OpenAIRE AMKE
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

package gr.uoa.di.madgik.registry.domain;

import gr.uoa.di.madgik.registry.domain.index.IndexField;
import java.util.ArrayList;
import java.util.List;

public final class ResourceEmbeddingChunker {

    private ResourceEmbeddingChunker() {
    }

    public static List<ResourceEmbeddingChunk> chunk(Resource resource, List<IndexField> indexFields) {
        if (resource == null || resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()
                || indexFields == null || indexFields.isEmpty()) {
            return List.of();
        }

        List<ResourceEmbeddingChunk> chunks = new ArrayList<>();
        int chunkIdx = 0;

        for (ResourceEmbeddingTextPreparer.PreparedField preparedField
                : ResourceEmbeddingTextPreparer.prepare(resource, indexFields, indexField -> true)) {
            IndexField indexField = preparedField.indexField();
            for (ResourceEmbeddingTextPreparer.PreparedValue preparedValue : preparedField.values()) {
                for (int fieldChunkIdx = 0; fieldChunkIdx < preparedValue.parts().size(); fieldChunkIdx++) {
                    String content = preparedValue.parts().get(fieldChunkIdx);
                    chunks.add(new ResourceEmbeddingChunk(
                            chunkIdx++,
                            indexField.getName(),
                            preparedValue.valueOrdinal(),
                            fieldChunkIdx,
                            content,
                            ResourceEmbeddingTextPreparer.semanticText(indexField, content)
                    ));
                }
            }
        }

        return chunks;
    }
}
