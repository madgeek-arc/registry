# Registry — Operations

This applies to `registry-service` and to applications embedding `registry-starter-service`.

## Start / stop

At startup, in order of observable effect: Flyway migrations, embedding-model loading and
Elasticsearch client creation (while beans are created), then the runners: resource-type loading
(`ResourceTypeInit`), creation of missing type views (`ResourceTypeViewInit`) and embedding warm-up
(`EmbeddingWarmup`). Once the application is ready, the DB↔index check starts on a virtual thread
(`SearchIndexConsistencyService`). A normal `SIGTERM`
stops the application with Spring Boot's graceful shutdown (on by default since Boot 3.4, not overridden
here). A running reconciliation is interrupted.

`registry-service` started in about 7 s against a local PostgreSQL with Elasticsearch and JMS
disabled.

## Health

`spring-boot-starter-actuator` is included, so `/actuator/health` is available with Spring Boot's
default exposure. Datasource health covers the primary `registryDataSource`. Elasticsearch health
isn't contributed: the starter builds its own `Rest5Client`, and `spring-boot-elasticsearch`, which holds
Spring Boot's Elasticsearch health indicator, isn't a dependency.

## Logging

Logback through SLF4J (Spring Boot default). Notable messages:

| Logger | Level | Meaning |
|---|---|---|
| `...monitor.ResourceMonitor` | ERROR `Error notifying listener` | Versioning, chunk embedding, indexing or JMS failed after a DB change, so the stores are now inconsistent |
| `...elasticsearch.SearchIndexConsistencyService` | INFO `Reindexing N missing resources`, ERROR `Database is missing the following resources`, WARN `Elasticsearch index '...' is missing` | Startup reconciliation results |
| `...jms.JmsResourceListener` | WARN `JMS is NOT notified ...` | Resource-type changes aren't published |
| `...elasticsearch.autoconfigure.ElasticAutoConfiguration` | INFO `Elasticsearch REST transport created for [...]` | Client nodes |
| `...autoconfigure.RegistryEmbeddingServiceAutoConfiguration` | WARN `No EmbeddingService found` | No embedding model, so semantic search and recommendations are unavailable |
| `...controllers.advice.GlobalExceptionHandler` | ERROR `Unhandled exception while processing ...` | A request failed with a 500 |

## Metrics and alerting

Actuator's default metrics are available if exposed by the host. Alert on `Error notifying listener`,
`Unhandled exception while processing` and `Database is missing the following resources`.

## Background jobs

| Job | Trigger | Notes |
|---|---|---|
| `SearchIndexConsistencyService` reconciliation | Every startup | Can take long for large datasets |
| Spring Batch `dump` | `GET /dump/` | Partitioned per resource type. Thread pool of `2 × CPUs − 1` |
| Spring Batch `restore` | `POST /restore/` | Resource types first, then resources in chunks of `batch.chunkSize`, multi-threaded, skipping failures |

Batch job metadata is stored in the Spring Batch tables in the registry DB.

## Database maintenance and backups

- Back up PostgreSQL regularly. It is the system of record. Elasticsearch is derived and can be
  rebuilt.
- `resourceversion` grows with every update that changes content. There's no automatic pruning.
  `VersionService.updateVersion` supports history rewrite for erasure.
- `resource_chunk` holds one or more embedding rows per resource and grows with the data.
- Views `<type>_view` are regenerated when types change. Manual edits are lost.
