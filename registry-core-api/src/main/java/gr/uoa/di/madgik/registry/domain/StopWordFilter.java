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

package gr.uoa.di.madgik.registry.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Curated English stop-word list used by the SQL search backend (see {@code LexicalSearchStrategy}
 * implementations in {@code registry-starter-service}). Elasticsearch handles stop-words natively
 * through its own search analyzer and does not use this class.
 *
 * <p>The list is deliberately curated rather than a length heuristic: short but meaningful terms
 * such as "AI", "UK", "C", "R", "Go", or "TV" must never be dropped just because they are short.</p>
 */
public final class StopWordFilter {

    public static final Set<String> ENGLISH_STOP_WORDS = Set.of(
            "a", "an", "the", "in", "on", "at", "of", "to", "is", "are", "was", "were",
            "be", "been", "being", "with", "and", "or", "but", "not", "this", "that",
            "these", "those", "for", "from", "by", "as", "it", "its", "into", "than",
            "then", "such", "so", "no", "nor", "do", "does", "did", "has", "have", "had",
            "if", "about", "over", "under", "up", "down", "out", "off", "again", "further",
            "once", "all", "any", "both", "each", "few", "more", "most", "other", "some",
            "own", "same", "too", "very", "can", "will", "just"
    );

    private StopWordFilter() {
    }

    /**
     * Tokenizes the query and drops stop words, preserving original term casing and order.
     *
     * @param query the raw user query
     * @return the significant terms, or an empty list when the query is null/blank or every
     *         token is a stop word
     */
    public static List<String> filterStopWords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String token : query.split("\\W+")) {
            if (token.isEmpty() || ENGLISH_STOP_WORDS.contains(token.toLowerCase(Locale.ROOT))) {
                continue;
            }
            terms.add(token);
        }
        return List.copyOf(terms);
    }
}
