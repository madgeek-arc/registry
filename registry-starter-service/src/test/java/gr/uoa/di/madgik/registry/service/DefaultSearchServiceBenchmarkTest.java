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
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Manual benchmark harness for local search latency checks.")
class DefaultSearchServiceBenchmarkTest extends PostgreSqlTestContainerSupport {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSearchServiceBenchmarkTest.class);
    private static final int SEED_RESOURCE_COUNT = 400;
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 20;

    @MockitoBean
    EmbeddingService embeddingService;

    @MockitoBean
    AuditActorProvider auditActorProvider;

    @Autowired
    SearchService searchService;

    @Autowired
    ResourceService resourceService;

    @BeforeAll
    void seedBenchmarkDataset() {
        when(auditActorProvider.currentActor()).thenReturn("benchmark-test");

        for (int i = 0; i < SEED_RESOURCE_COUNT; i++) {
            String author = (i % 2 == 0 ? "Analytics" : "Operations") + " Benchmark " + i;
            int age = 25 + (i % 20);
            resourceService.addResource(newEmployeeResource(author, age));
        }
    }

    @Test
    void benchmarkSearch_withAndWithoutFacets() {
        Supplier<FacetFilter> withoutFacets = () -> employeeFilter(null);
        Supplier<FacetFilter> withFacets = () -> employeeFilter(List.of("age"));

        BenchmarkResult plainSearch = measure("search/no-facets", withoutFacets);
        BenchmarkResult facetedSearch = measure("search/with-facets", withFacets);

        assertEquals(plainSearch.total(), facetedSearch.total(),
                "Facet collection should not change the matched result count.");

        logger.info("Benchmark search/no-facets: {}", plainSearch.summary());
        logger.info("Benchmark search/with-facets: {}", facetedSearch.summary());
    }

    private BenchmarkResult measure(String label, Supplier<FacetFilter> filterSupplier) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            searchService.search(filterSupplier.get());
        }

        long totalNanos = 0;
        long minNanos = Long.MAX_VALUE;
        long maxNanos = Long.MIN_VALUE;
        int total = -1;

        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            Paging<Resource> result = searchService.search(filterSupplier.get());
            long elapsed = System.nanoTime() - start;

            total = result.getTotal();
            totalNanos += elapsed;
            minNanos = Math.min(minNanos, elapsed);
            maxNanos = Math.max(maxNanos, elapsed);
        }

        return new BenchmarkResult(label, total, totalNanos, minNanos, maxNanos, MEASURED_RUNS);
    }

    private FacetFilter employeeFilter(List<String> browseBy) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType("employee");
        filter.setKeyword("Analytics");
        filter.setQuantity(25);
        if (browseBy != null) {
            filter.setBrowseBy(browseBy);
        }
        return filter;
    }

    private Resource newEmployeeResource(String author, int age) {
        Resource resource = new Resource();
        resource.setResourceTypeName("employee");
        resource.setPayloadFormat("xml");
        resource.setPayload("""
                <?xml version="1.0"?>
                <employee>
                  <author>%s</author>
                  <age>%d</age>
                  <single>false</single>
                  <birthday>645544821000</birthday>
                  <salary>1292.123</salary>
                  <amka>%d</amka>
                </employee>
                """.formatted(author, age, Math.abs(author.hashCode()) + 10000000000000L));
        return resource;
    }

    private record BenchmarkResult(
            String label,
            int total,
            long totalNanos,
            long minNanos,
            long maxNanos,
            int measuredRuns
    ) {
        String summary() {
            return "%s total=%d avg=%s min=%s max=%s runs=%d".formatted(
                    label,
                    total,
                    formatNanos(totalNanos / measuredRuns),
                    formatNanos(minNanos),
                    formatNanos(maxNanos),
                    measuredRuns
            );
        }

        private static String formatNanos(long nanos) {
            return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0d);
        }
    }
}
