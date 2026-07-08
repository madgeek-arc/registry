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

package gr.uoa.di.madgik.registry.elasticsearch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the registry Elasticsearch starter.
 *
 * <p>Bind these in your {@code application.yml}:
 * <pre>{@code
 * registry:
 *   elasticsearch:
 *     uris:
 *       - https://es-host:9200
 *     username: elastic
 *     password: secret
 *     aggregation:
 *       top-hits-size: 100
 *       bucket-size: 100
 *     index:
 *       max-result-window: 10000
 *       bulk-batch-size: 100
 *     search:
 *       highlight:
 *         fragment-size: 400
 *         number-of-fragments: 5
 * }</pre>
 *
 * <p>At least one URI must be provided. When {@code username} and {@code password} are both set,
 * HTTP Basic authentication is applied to every request via the Elasticsearch Java client.
 */
@ConfigurationProperties("registry.elasticsearch")
public class RegistryElasticsearchProperties {

    /**
     * List of Elasticsearch node URIs (scheme + host + port).
     * At least one URI is required for the starter to connect.
     * Example: {@code https://es-node1:9200}.
     */
    private List<String> uris = new ArrayList<>();

    /**
     * Username for HTTP Basic authentication against Elasticsearch.
     * Leave blank to disable authentication.
     */
    private String username;

    /**
     * Password for HTTP Basic authentication against Elasticsearch.
     * Leave blank to disable authentication.
     */
    private String password;

    private final Aggregation aggregation = new Aggregation();
    private final Index index = new Index();
    private final Search search = new Search();

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris == null ? new ArrayList<>() : new ArrayList<>(uris);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public Index getIndex() {
        return index;
    }

    public Search getSearch() {
        return search;
    }

    /**
     * Aggregation (facet) query tuning parameters.
     */
    public static class Aggregation {
        /**
         * Maximum number of hits to collect inside a {@code top_hits} sub-aggregation.
         * Raising this value returns more matched documents per facet bucket but increases
         * memory pressure on the Elasticsearch node.
         */
        private int topHitsSize = 100;

        /**
         * Maximum number of buckets to return per terms aggregation (facet).
         * Corresponds to the {@code size} parameter of an Elasticsearch {@code terms} aggregation.
         * Raising this value increases the cardinality of returned facet values.
         */
        private int bucketSize = 100;

        public int getTopHitsSize() {
            return topHitsSize;
        }

        public void setTopHitsSize(int topHitsSize) {
            this.topHitsSize = topHitsSize;
        }

        public int getBucketSize() {
            return bucketSize;
        }

        public void setBucketSize(int bucketSize) {
            this.bucketSize = bucketSize;
        }
    }

    /**
     * Index-level settings applied when creating or querying registry indices.
     */
    public static class Index {
        /**
         * Value used as the {@code max_result_window} index setting and as the upper bound for
         * {@code from + size} in search requests. Elasticsearch's default is {@code 10000};
         * increase with caution as large windows require more heap on the coordinating node.
         */
        private int maxResultWindow = 10000;

        /**
         * Maximum number of resources sent per Elasticsearch bulk request. Large resource types are
         * split into sub-batches of this size so a single HTTP request doesn't exceed proxy/gateway
         * body-size limits. Lower this if you see HTTP 413 errors during resource-type re-index or bulk
         * restore; raise it to reduce the number of round-trips for large collections.
         */
        private int bulkBatchSize = 100;

        public int getMaxResultWindow() {
            return maxResultWindow;
        }

        public void setMaxResultWindow(int maxResultWindow) {
            this.maxResultWindow = maxResultWindow;
        }

        public int getBulkBatchSize() {
            return bulkBatchSize;
        }

        public void setBulkBatchSize(int bulkBatchSize) {
            this.bulkBatchSize = bulkBatchSize;
        }
    }

    /**
     * Search-query tuning parameters.
     */
    public static class Search {
        private final Highlight highlight = new Highlight();

        public Highlight getHighlight() {
            return highlight;
        }
    }

    /**
     * Highlight settings for the Elasticsearch search backend.
     */
    public static class Highlight {
        /**
         * Size in characters of each highlight fragment returned by Elasticsearch.
         * Maps directly to the {@code fragment_size} highlight parameter.
         */
        private int fragmentSize = 400;

        /**
         * Maximum number of highlight fragments to return per field per result.
         * Maps directly to the {@code number_of_fragments} highlight parameter.
         */
        private int numberOfFragments = 5;

        public int getFragmentSize() {
            return fragmentSize;
        }

        public void setFragmentSize(int fragmentSize) {
            this.fragmentSize = fragmentSize;
        }

        public int getNumberOfFragments() {
            return numberOfFragments;
        }

        public void setNumberOfFragments(int numberOfFragments) {
            this.numberOfFragments = numberOfFragments;
        }
    }
}
