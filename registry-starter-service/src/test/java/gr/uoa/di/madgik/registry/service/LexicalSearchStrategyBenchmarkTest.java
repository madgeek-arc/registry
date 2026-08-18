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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.service.lexical.FullTextLexicalStrategy;
import gr.uoa.di.madgik.registry.service.lexical.LexicalMatch;
import gr.uoa.di.madgik.registry.service.lexical.LexicalSearchStrategy;
import gr.uoa.di.madgik.registry.service.lexical.TermOrLikeLexicalStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual comparison harness for the two {@link gr.uoa.di.madgik.registry.service.lexical.LexicalSearchStrategy}
 * implementations. Runs a fixed set of representative queries against both strategies directly
 * (bypassing {@code LexicalSearchStrategySelector}/the configured default) over a
 * prose-bearing dataset, logging timing and matched-ID overlap so the two can be compared before
 * choosing a default for {@code registry.sql.search.lexical-strategy}.
 */
@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Manual benchmark harness for comparing the term-like and full-text lexical strategies.")
class LexicalSearchStrategyBenchmarkTest extends PostgreSqlTestContainerSupport {

    private static final Logger logger = LoggerFactory.getLogger(LexicalSearchStrategyBenchmarkTest.class);
    private static final int SEED_RESOURCE_COUNT = 5000;
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 20;

    private static final List<String> BENCHMARK_QUERIES = List.of(
            "data",
            "senior data platform engineer",
            "the analytics team responsible for the reporting pipeline",
            "quantum-resistant cryptography"
    );

    private static final List<String> PROSE_PARAGRAPHS = List.of(
            "Senior data platform engineer with a decade of experience building resilient analytics "
                    + "pipelines that keep the reporting stack online through peak traffic.",
            "Operations lead focused on infrastructure reliability, on-call rotations, and reducing "
                    + "the time it takes the incident response team to restore a degraded service.",
            "Machine learning researcher exploring quantum-resistant cryptography and its implications "
                    + "for long-term data retention across the analytics warehouse.",
            "Frontend engineer specializing in accessible dashboard design for the reporting pipeline "
                    + "used daily by the operations and analytics teams."
    );

    @MockitoBean
    EmbeddingService embeddingService;

    @MockitoBean
    AuditActorProvider auditActorProvider;

    @Autowired
    TermOrLikeLexicalStrategy termLikeStrategy;

    @Autowired
    FullTextLexicalStrategy fullTextStrategy;

    @Autowired
    @Qualifier("registryDataSource")
    DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeAll
    void seedProseBearingDataset() {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        String sql = """
                INSERT INTO public.resource
                  (id, fk_name, version, payload, payloadformat, creation_date, modification_date, created_by, modified_by)
                VALUES (:id, :fk_name, :version, :payload, :payloadformat, :creation_date, :modification_date, :created_by, :modified_by)
                """;
        MapSqlParameterSource[] rows = new MapSqlParameterSource[SEED_RESOURCE_COUNT];
        for (int i = 0; i < SEED_RESOURCE_COUNT; i++) {
            String bio = PROSE_PARAGRAPHS.get(i % PROSE_PARAGRAPHS.size()) + " (candidate " + i + ")";
            rows[i] = employeeResourceParams(bio, 25 + (i % 20));
        }
        jdbcTemplate.batchUpdate(sql, rows);
    }

    @Test
    void compareStrategies_acrossRepresentativeQueries() {
        for (String query : BENCHMARK_QUERIES) {
            BenchmarkResult termLike = measure("term-like", termLikeStrategy, query);
            BenchmarkResult fullText = measure("full-text", fullTextStrategy, query);

            Set<String> onlyInTermLike = new TreeSet<>(termLike.matchedIds());
            onlyInTermLike.removeAll(fullText.matchedIds());
            Set<String> onlyInFullText = new TreeSet<>(fullText.matchedIds());
            onlyInFullText.removeAll(termLike.matchedIds());

            logger.info("Query '{}': {}", query, termLike.summary());
            logger.info("Query '{}': {}", query, fullText.summary());
            logger.info("Query '{}': matched only by term-like={}, only by full-text={}",
                    query, onlyInTermLike.size(), onlyInFullText.size());

            assertTrue(termLike.matchedIds().size() >= 0 && fullText.matchedIds().size() >= 0);
        }
    }

    private BenchmarkResult measure(String label, LexicalSearchStrategy strategy, String query) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            runOnce(strategy, query);
        }

        long totalNanos = 0;
        List<String> matchedIds = List.of();
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            matchedIds = runOnce(strategy, query);
            totalNanos += System.nanoTime() - start;
        }

        return new BenchmarkResult(label, matchedIds, totalNanos / MEASURED_RUNS);
    }

    private List<String> runOnce(LexicalSearchStrategy strategy, String query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        LexicalMatch match = strategy.build(query, params);
        String sql = "SELECT id FROM resource WHERE fk_name = 'employee' AND %s"
                .formatted(match.wherePredicate("resource"));
        return jdbcTemplate.queryForList(sql, params, String.class);
    }

    private MapSqlParameterSource employeeResourceParams(String author, int age) {
        String payload = """
                <?xml version="1.0"?>
                <employee>
                  <author>%s</author>
                  <age>%d</age>
                  <single>false</single>
                  <birthday>645544821000</birthday>
                  <salary>1292.123</salary>
                  <amka>%d</amka>
                </employee>
                """.formatted(author, age, Math.abs(author.hashCode()) + 10000000000000L);
        Timestamp now = Timestamp.from(Instant.now());
        return new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID().toString())
                .addValue("fk_name", "employee")
                .addValue("version", UUID.randomUUID().toString())
                .addValue("payload", payload)
                .addValue("payloadformat", "xml")
                .addValue("creation_date", now)
                .addValue("modification_date", now)
                .addValue("created_by", "system")
                .addValue("modified_by", "system");
    }

    private record BenchmarkResult(String label, List<String> matchedIds, long avgNanos) {
        String summary() {
            return "%s matched=%d avg=%s".formatted(label, matchedIds.size(), formatNanos(avgNanos));
        }

        private static String formatNanos(long nanos) {
            return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0d);
        }
    }
}
