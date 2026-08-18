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

package gr.uoa.di.madgik.registry.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for PostgreSQL-backed search.
 *
 * <p>Bind these in your {@code application.yml}:
 * <pre>{@code
 * registry:
 *   sql:
 *     search:
 *       semantic-min-score: 0.3
 *       lexical-strategy: full-text
 *       highlight:
 *         payload-context-chars: 80
 *         payload-max-fragments: 5
 *         semantic-max-chars: 220
 * }</pre>
 */
@ConfigurationProperties("registry.sql.search")
public class SqlSearchProperties {

    /**
     * Minimum cosine-similarity score required for a resource to be included in semantic
     * retrieval. The score is computed as {@code 1 - cosine_distance}, so higher is better
     * and the valid range is {@code [-1, 1]}.
     */
    private float semanticMinScore = 0.3f;

    /**
     * Which {@code LexicalSearchStrategy} bean serves lexical matching for {@code search()} and
     * hybrid search's lexical part, matched against a strategy's {@code name()}: {@code full-text}
     * (default — Postgres {@code tsvector}/{@code tsquery}).
     */
    private String lexicalStrategy = "full-text";

    private final Highlight highlight = new Highlight();

    public float getSemanticMinScore() {
        return semanticMinScore;
    }

    public void setSemanticMinScore(float semanticMinScore) {
        this.semanticMinScore = semanticMinScore;
    }

    public String getLexicalStrategy() {
        return lexicalStrategy;
    }

    public void setLexicalStrategy(String lexicalStrategy) {
        this.lexicalStrategy = lexicalStrategy;
    }

    public Highlight getHighlight() {
        return highlight;
    }

    public static class Highlight {

        private int payloadContextChars = 80;

        private int payloadMaxFragments = 5;

        private int semanticMaxChars = 220;

        public int getPayloadContextChars() {
            return payloadContextChars;
        }

        public void setPayloadContextChars(int payloadContextChars) {
            this.payloadContextChars = payloadContextChars;
        }

        public int getPayloadMaxFragments() {
            return payloadMaxFragments;
        }

        public void setPayloadMaxFragments(int payloadMaxFragments) {
            this.payloadMaxFragments = payloadMaxFragments;
        }

        public int getSemanticMaxChars() {
            return semanticMaxChars;
        }

        public void setSemanticMaxChars(int semanticMaxChars) {
            this.semanticMaxChars = semanticMaxChars;
        }
    }
}
