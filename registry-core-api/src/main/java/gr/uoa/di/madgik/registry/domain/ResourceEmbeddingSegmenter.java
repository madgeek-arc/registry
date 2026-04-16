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

package gr.uoa.di.madgik.registry.domain;

import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.ArrayList;
import java.util.List;

public final class ResourceEmbeddingSegmenter {

    private ResourceEmbeddingSegmenter() {
    }

    public static List<Segment> segment(Resource resource, List<IndexField> indexFields) {
        if (resource == null || resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()
                || indexFields == null || indexFields.isEmpty()) {
            return List.of();
        }

        List<Segment> segments = new ArrayList<>();
        for (ResourceEmbeddingTextPreparer.PreparedField preparedField
                : ResourceEmbeddingTextPreparer.prepare(resource, indexFields,
                indexField -> indexField.getEmbeddingWeight() > 0)) {
            IndexField indexField = preparedField.indexField();
            List<String> values = new ArrayList<>();
            for (ResourceEmbeddingTextPreparer.PreparedValue preparedValue : preparedField.values()) {
                values.addAll(preparedValue.parts());
            }

            if (!values.isEmpty()) {
                values.sort(String.CASE_INSENSITIVE_ORDER);
                segments.add(new Segment(
                        ResourceEmbeddingTextPreparer.resolveLabel(indexField),
                        indexField.getEmbeddingWeight(),
                        values
                ));
            }
        }

        return segments;
    }
}
