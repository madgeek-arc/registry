# Registry — Testing

Tests use JUnit 5, AssertJ, Mockito and Spring Boot's test support. Database and Elasticsearch tests
run against Testcontainers (`pgvector/pgvector:pg16`, `docker.elastic.co/elasticsearch/elasticsearch:9.4.3`),
so **Docker is required**.

| Module | Tests | Scope |
|---|---|---|
| `registry-core-api` | `StopWordFilterTest` | Search helpers |
| `registry-core-helper` | `GenericResourceManagerTest`, `FacetLabelServiceTest` | Typed-resource helpers |
| `registry-core-rest` | `GlobalExceptionHandlerTest` | Status mapping of registry and Spring MVC exceptions |
| `registry-starter-client` | `RegistryClientAutoConfigurationTest` | Default timeouts, calls through the shared `RestTemplate` to an in-JVM HTTP server, read-timeout failure, application `RestTemplate` override |
| `registry-starter-elasticsearch` | `ElasticAutoConfigurationTest`, `ElasticSearchServiceIntegrationTest` (Testcontainers), mapping, bulk, query and chunking tests | Bean wiring, multi-node URIs and default ports, indexing and search against a real Elasticsearch |
| `registry-starter-jms` | `JmsResourceListenerTest`, `JmsAutoConfigurationTest` | Topics and payloads for create/update/delete/type change, broker settings, back-off when disabled (also when the nested configuration is component-scanned) |
| `registry-starter-service` | DAO tests, `ResourceServiceImplTest`, `ResourceTypeService*Test`, `DefaultSearchService*Test` (query building, full-text, semantic, range filters, connection leaks), lexical strategy tests, view and startup tests, `TypedResourceController*IntegrationTest`, encoded-slash tests, `RestControllers*IntegrationTest`, `ResourceSyncServiceImplTest`, `RegistryEmbeddingModelDefaultsTest`, batch and Hibernate configuration tests | Service behaviour on PostgreSQL with pgvector, REST wiring and switches, ResourceSync URLs, embedding-model defaults |

Test configuration: `registry-starter-service/src/test/resources/application-test.properties`
(`hbm2ddl.auto=create-drop`, Flyway disabled, SQL logging) with SQL fixtures in the same folder.

## Running

```bash
mvn -B -Ddependency-check.skip=true test
mvn -B -Ddependency-check.skip=true -pl registry-starter-service test -Dtest=ResourceServiceImplTest
```

Result on `fix/report-issues`: 301 tests, 0 failures, 0 errors, 2 skipped (the manual
`DefaultSearchServiceBenchmarkTest` and `LexicalSearchStrategyBenchmarkTest`). By module: core-api 5,
core-helper 7, core-rest 5, client 4, elasticsearch 58, jms 9, service 213.

## Not covered

- Spring Batch dump/restore end to end (only the restore reader step and batch configuration are
  tested).
- JMS against a real broker (the listener and auto-configuration are tested with mocks).
- The other client service implementations beyond `ResourceService#getResource`.
- Flyway migrations: tests build the schema with Hibernate (`create-drop`). The migrations were run
  once against `pgvector/pgvector:pg16` when starting `registry-service` manually.
- No coverage tool and no CI configuration in the repository.
