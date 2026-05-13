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
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.function.Predicate;

final class ResourceEmbeddingTextPreparer {

    private static final Logger logger = LoggerFactory.getLogger(ResourceEmbeddingTextPreparer.class);
    private static final int MAX_CHUNK_TOKENS = 120;
    private static final int WINDOW_OVERLAP_TOKENS = 20;

    private ResourceEmbeddingTextPreparer() {
    }

    static List<PreparedField> prepare(Resource resource,
                                       List<IndexField> indexFields,
                                       Predicate<IndexField> fieldFilter) {
        if (resource == null || resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()
                || indexFields == null || indexFields.isEmpty()) {
            return List.of();
        }

        Map<String, IndexedField<?>> valuesByName = new LinkedHashMap<>();
        for (IndexedField<?> indexedField : resource.getIndexedFields()) {
            valuesByName.put(indexedField.getName(), indexedField);
        }

        List<PreparedField> preparedFields = new ArrayList<>();
        for (IndexField indexField : indexFields) {
            if (!"java.lang.String".equals(indexField.getType())) {
                if (fieldFilter.test(indexField)) {
                    logger.warn("IndexField '{}' has embeddingWeight > 0 but type '{}' cannot be embedded; skipping.",
                            indexField.getName(), indexField.getType());
                }
                continue;
            }
            if (!fieldFilter.test(indexField)) {
                continue;
            }

            IndexedField<?> indexedField = valuesByName.get(indexField.getName());
            if (indexedField == null || indexedField.getValues() == null || indexedField.getValues().isEmpty()) {
                continue;
            }

            List<String> normalizedValues = indexedField.getValues().stream()
                    .map(ResourceEmbeddingTextPreparer::normalizeValue)
                    .filter(value -> !value.isBlank())
                    .sorted(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)))
                    .toList();

            List<PreparedValue> preparedValues = new ArrayList<>();
            for (int valueOrdinal = 0; valueOrdinal < normalizedValues.size(); valueOrdinal++) {
                List<String> parts = splitValue(indexField, normalizedValues.get(valueOrdinal));
                if (!parts.isEmpty()) {
                    preparedValues.add(new PreparedValue(valueOrdinal, parts));
                }
            }

            if (!preparedValues.isEmpty()) {
                preparedFields.add(new PreparedField(indexField, preparedValues));
            }
        }

        return preparedFields;
    }

    /**
     * Splits a single field value into embeddable chunks.
     * Strategy: for TEXT-capable, single-valued, non-PK String fields, the value is first split into
     * sentences; each sentence is then independently split into overlapping token windows
     * ({@value MAX_CHUNK_TOKENS} tokens, {@value WINDOW_OVERLAP_TOKENS}-token overlap).
     * Cross-sentence overlap is intentionally not applied — sentence boundaries are treated as
     * natural semantic breaks.
     */
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
                    .map(ResourceEmbeddingTextPreparer::normalizeValue)
                    .filter(text -> !text.isBlank())
                    .reduce((left, right) -> left + " " + right)
                    .orElse("");
        }
        return Objects.toString(value, "");
    }

    static String resolveLabel(IndexField indexField) {
        String label = indexField.getLabel();
        if (label != null && !label.isBlank()) {
            return label;
        }
        return indexField.getName() == null ? "" : indexField.getName();
    }

    static String semanticText(IndexField indexField, String content) {
        String label = resolveLabel(indexField);
        return label.isBlank() ? content : label + ": " + content;
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

    record PreparedField(IndexField indexField, List<PreparedValue> values) {
    }

    record PreparedValue(int valueOrdinal, List<String> parts) {
    }
}
