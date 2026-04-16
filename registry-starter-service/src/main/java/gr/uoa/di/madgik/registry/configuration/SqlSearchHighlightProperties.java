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
 * Configuration properties for SQL search result highlighting.
 *
 * <p>Bind these in your {@code application.yml}:
 * <pre>{@code
 * registry:
 *   sql:
 *     search:
 *       highlight:
 *         payload-context-chars: 80
 *         payload-max-fragments: 5
 *         semantic-max-chars: 220
 * }</pre>
 *
 * <p>This group controls how highlight snippets are extracted from the raw payload column in the
 * PostgreSQL-backed search implementation.
 */
@ConfigurationProperties("registry.sql.search.highlight")
public class SqlSearchHighlightProperties {

    /**
     * Number of characters of surrounding context to include on each side of a keyword match
     * when extracting highlight fragments from the raw payload column.
     */
    private int payloadContextChars = 80;

    /**
     * Maximum number of highlight fragments to return per result when highlighting the raw
     * payload column via keyword search.
     */
    private int payloadMaxFragments = 5;

    /**
     * Maximum total characters to include in a highlight snippet returned for a semantic
     * (vector/embedding) search hit. The nearest matching chunk text is trimmed to this
     * length before being returned.
     */
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
