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

package gr.uoa.di.madgik.registry.domain.index;

/**
 * Declares what search operations a {@code java.lang.String} {@link gr.uoa.di.madgik.registry.domain.index.IndexField}
 * supports. Non-string fields ignore this setting entirely.
 *
 * <p>The two capabilities are not mutually exclusive; declaring both gives a field all
 * behaviours described below. When {@code searchCapabilities} is omitted or empty on a newly
 * created resource type, {@link #KEYWORD} behaviour applies by default.</p>
 *
 * <p><b>Facet eligibility rule</b>: a field is included in the auto-derived {@code browseBy}
 * list only when it has {@link #KEYWORD} capability <em>and</em> a non-null {@code label}.
 * A TEXT-only field is intentionally excluded from facets because faceting requires exact-value
 * grouping, which is a keyword-style operation.</p>
 */
public enum SearchCapability {

    /**
     * The field value is treated as a single unit for exact-match operations.
     *
     * <ul>
     *   <li><b>Elasticsearch</b>: mapped as {@code keyword}; participates in multi-match lexical
     *       queries, exact-value filters, aggregations, and sorting on the base field.</li>
     *   <li><b>SQL</b>: fields are included in SQL keyword highlight results.</li>
     *   <li><b>Facets</b>: required (together with a non-null {@code label}) for a field to
     *       appear in the auto-derived {@code browseBy} list and for
     *       {@code FacetLabelService} label enrichment.</li>
     *   <li><b>Embeddings</b>: the field value is embedded as one chunk per value with no
     *       sentence splitting.</li>
     * </ul>
     */
    KEYWORD,

    /**
     * The field value is handled as free-form prose for full-text search.
     *
     * <ul>
     *   <li><b>Elasticsearch</b>: adds an analyzed {@code .text} sub-field (custom English
     *       analyzer) on top of the base {@code keyword} field, enabling full-text matching
     *       and highlighting via {@code fieldName.text}.</li>
     *   <li><b>SQL</b>: has no effect on lexical search <em>scope</em> — the SQL backend uses
     *       payload-wide {@code LIKE} matching regardless — but TEXT fields are included in SQL
     *       keyword highlight results alongside {@link #KEYWORD} fields.</li>
     *   <li><b>Facets</b>: does <em>not</em> grant facet eligibility on its own; combine with
     *       {@link #KEYWORD} when the field should also appear as a browse facet.</li>
     *   <li><b>Embeddings</b>: single-valued non-PK field values are sentence-split; sentences
     *       longer than 120 tokens are further split into overlapping 120-token windows before
     *       embedding.</li>
     * </ul>
     *
     * <p><b>Elasticsearch mapping note</b>: even a TEXT-only field retains a {@code keyword}
     * base type in the index (required for aggregations), so raw Elasticsearch term aggregations
     * still function. The capability describes <em>intended usage</em>, not a strict physical
     * mapping constraint.</p>
     */
    TEXT
}
