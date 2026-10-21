# Registry — Operations

This applies to `registry-service` and to applications embedding `registry-starter-service`.

## Start / stop

At startup, in order of observable effect: Flyway migrations, resource-type loading
(`ResourceTypeInit`), Elasticsearch client creation, background DB↔index reconciliation on a virtual
thread (`IndexDbSync`), and embedding-model warm-up. A normal `SIGTERM` stops the application with
Spring Boot's graceful shutdown (on by default since Boot 3.4, not overridden here). A reconciliation
still running is interrupted.

## Health

`spring-boot-starter-actuator` is included, so `/actuator/health` is available with Spring Boot's
default exposure. Datasource health covers the primary `registryDataSource`. The `elasticsearch` health
indicator doesn't check the registry's cluster: the starter creates its own `RestHighLevelClient`, and
Spring Boot, because the low-level client is on the class path, also creates a `RestClient` from
`spring.elasticsearch.uris` (default `http://localhost:9200`) and checks that one. Set
`spring.elasticsearch.uris` to the registry's nodes, or disable the indicator with
`management.health.elasticsearch.enabled=false`.

## Logging

Log4j2 through SLF4J. Notable messages:

| Logger | Level | Meaning |
|---|---|---|
| `...monitor.ResourceMonitor` | ERROR `Error notifying listener` | Indexing, versioning or JMS failed after a DB change, so the stores are now inconsistent |
| `...elasticsearch.IndexDbSync` | INFO `Reindexing N missing resources`, ERROR `Database is missing the following resources` | Startup reconciliation results |
| `...registry_starter.jms.JmsResourceListener` | WARN `JMS is NOT notified ...` | Resource-type changes aren't published |
| `...autoconfigure.ElasticAutoConfiguration` | INFO/WARN | Client target and embedding warm-up |
| `...service.ResourceSyncServiceImpl` | stdout | Prints `registry.host` at startup (`System.out.println`) |

## Metrics and alerting

Actuator's default metrics are available if exposed by the host. Alert on `Error notifying listener`
and on `Database is missing the following resources`.

## Background jobs

| Job | Trigger | Notes |
|---|---|---|
| `IndexDbSync` reconciliation | Every startup | Can be long for large datasets: resources are re-added one by one (code `TODO` mentions bulk) |
| Spring Batch `dump` | `GET /dump/` | Partitioned per resource type. Thread pool of `2 × CPUs − 1` |
| Spring Batch `restore` | `POST /restore/` | Resource types first, then resources in chunks of `batch.chunkSize`, multi-threaded, skipping failures |

Batch job metadata is stored in the Spring Batch tables in the registry DB.

## Database maintenance and backups

- Back up PostgreSQL regularly. It is the system of record. Elasticsearch is derived and can be
  rebuilt.
- `ResourceVersion` grows with every update. There's no automatic pruning. `VersionService.updateVersion`
  supports history rewrite for erasure (changelog `4.3.0`).
- Views `<type>_view` are regenerated when types change. Manual edits are lost.
