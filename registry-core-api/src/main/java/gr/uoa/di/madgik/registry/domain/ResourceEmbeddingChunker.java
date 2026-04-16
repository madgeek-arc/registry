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
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;

import java.text.BreakIterator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ResourceEmbeddingChunker {

    private static final int MAX_CHUNK_TOKENS = 120;
    private static final int WINDOW_OVERLAP_TOKENS = 20;

    private ResourceEmbeddingChunker() {
    }

    public static List<ResourceEmbeddingChunk> chunk(Resource resource, List<IndexField> indexFields) {
        if (resource == null || resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()
                || indexFields == null || indexFields.isEmpty()) {
            return List.of();
        }

        Map<String, IndexedField<?>> valuesByName = new LinkedHashMap<>();
        for (IndexedField<?> indexedField : resource.getIndexedFields()) {
            valuesByName.put(indexedField.getName(), indexedField);
        }

        List<ResourceEmbeddingChunk> chunks = new ArrayList<>();
        int chunkIdx = 0;

        for (IndexField indexField : indexFields) {
            if (!"java.lang.String".equals(indexField.getType())) {
                continue;
            }
            IndexedField<?> indexedField = valuesByName.get(indexField.getName());
            if (indexedField == null || indexedField.getValues() == null || indexedField.getValues().isEmpty()) {
                continue;
            }

            List<String> values = indexedField.getValues().stream()
                    .map(ResourceEmbeddingChunker::normalizeValue)
                    .filter(value -> !value.isBlank())
                    .sorted(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)))
                    .toList();

            for (int valueOrdinal = 0; valueOrdinal < values.size(); valueOrdinal++) {
                List<String> splitChunks = splitValue(indexField, values.get(valueOrdinal));
                for (int fieldChunkIdx = 0; fieldChunkIdx < splitChunks.size(); fieldChunkIdx++) {
                    String content = splitChunks.get(fieldChunkIdx);
                    chunks.add(new ResourceEmbeddingChunk(
                            chunkIdx++,
                            indexField.getName(),
                            valueOrdinal,
                            fieldChunkIdx,
                            content,
                            semanticText(indexField, content)
                    ));
                }
            }
        }

        return chunks;
    }

    static List<String> splitValue(IndexField indexField, String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        if (!isSentenceSplitCandidate(indexField)) {
            return List.of(normalized);
        }

        List<String> sentences = splitSentences(normalized);
        if (sentences.size() <= 1) {
            return splitOversizedSentence(normalized);
        }

        List<String> chunks = new ArrayList<>();
        for (String sentence : sentences) {
            chunks.addAll(splitOversizedSentence(sentence));
        }
        return chunks;
    }

    static boolean isSentenceSplitCandidate(IndexField indexField) {
        return "java.lang.String".equals(indexField.getType())
                && indexField.hasSearchCapability(SearchCapability.TEXT)
                && !indexField.isMultivalued()
                && !indexField.isPrimaryKey();
    }

    private static List<String> splitSentences(String text) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(text);
        List<String> sentences = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).replaceAll("\\s+", " ").trim();
            if (!sentence.isBlank()) {
                sentences.add(sentence);
            }
        }
        return sentences.isEmpty() ? List.of(text) : sentences;
    }

    private static List<String> splitOversizedSentence(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        String[] tokens = normalized.split("\\s+");
        if (tokens.length <= MAX_CHUNK_TOKENS) {
            return List.of(normalized);
        }

        int step = Math.max(1, MAX_CHUNK_TOKENS - WINDOW_OVERLAP_TOKENS);
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < tokens.length; start += step) {
            int end = Math.min(tokens.length, start + MAX_CHUNK_TOKENS);
            String chunk = String.join(" ", Arrays.copyOfRange(tokens, start, end)).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end == tokens.length) {
                break;
            }
        }
        return chunks;
    }

    private static String semanticText(IndexField indexField, String content) {
        String label = indexField.getLabel();
        String prefix = label != null && !label.isBlank() ? label : indexField.getName();
        return prefix + ": " + content;
    }

    static String normalizeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof Date date) {
            return date.toInstant().toString();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(ResourceEmbeddingChunker::normalizeValue)
                    .filter(text -> !text.isBlank())
                    .reduce((left, right) -> left + " " + right)
                    .orElse("");
        }
        return Objects.toString(value, "");
    }
}
