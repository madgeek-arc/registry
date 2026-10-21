# Registry — Deployment

The repository contains no Dockerfile, compose file, Kubernetes manifest or CI pipeline. It builds
Maven artefacts and one executable jar. Applications that embed the starters deploy the registry as
part of themselves, and are responsible for securing its REST paths (see [security.md](security.md)).

Production deployment isn't defined in this repository. `registry-service` can run as the executable
jar managed as a system service, or in a container started with Docker Compose or deployed on
Kubernetes.

## Prerequisites

| Dependency | Required | Notes |
|---|---|---|
| JDK 25 | Yes | |
| Maven 3.9+ | Build | No wrapper in the repository |
| PostgreSQL with pgvector | Yes | The DB user must be allowed to `CREATE EXTENSION vector` and `tablefunc` (Flyway `V3.0`, `V5.0`), or the extensions must be pre-created. `pgvector/pgvector:pg16` works. |
| Elasticsearch 9.x | If `registry-starter-elasticsearch` is on the classpath and enabled | Java client 9.4.3. Tests run against Elasticsearch 9.4.3. |
| ActiveMQ Classic | If `registry-starter-jms` is on the classpath and enabled | Defaults to `tcp://localhost:61616` |
| Embedding model | — | Packaged in `registry-starter-service` (`all-MiniLM-L6-v2`, about 90 MB). Spring AI copies it to a temporary cache directory at startup. No network access is needed. |

## Build

```bash
mvn -B -Ddependency-check.skip=true clean install      # all modules + tests (needs Docker) + javadoc jars
mvn -B dependency-check:check                          # OWASP scan; fails at CVSS >= 7.6
```

| Artefact | Path |
|---|---|
| Starters/libraries | `gr.uoa.di.madgik:registry-*:<version>`, versions aligned by `registry-bom` (deployed to the madgik repository with `mvn deploy`) |
| Standalone service | `registry-service/target/RegistryService-<version>.jar` (Spring Boot executable, `Start-Class gr.uoa.di.madgik.registry.RegistryService`) |

Releases: `mvn release:prepare release:perform` creates tag `v<version>` and commits prefixed
`chore(release):`. (`pom.xml` `maven-release-plugin`)

## Running `registry-service`

```bash
java -jar registry-service/target/RegistryService-<version>.jar \
  --spring.config.additional-location=file:./application.properties
```

Without Elasticsearch or ActiveMQ, add `--registry.elasticsearch.enabled=false --registry.jms.enabled=false`.
(started against `pgvector/pgvector:pg16` with both disabled)

| Aspect | Value |
|---|---|
| Port | `8080` (Spring Boot default, override with `server.port`) |
| Volumes | Writable servlet temp/real path for dump ZIPs. Writable temp directory for the embedding-model cache |
| Network | PostgreSQL. Elasticsearch HTTP. ActiveMQ OpenWire. No outbound access needed for the model |
| Order | PostgreSQL → Elasticsearch/ActiveMQ → registry → consumers |
| Public URL | Set `registry.host` (or `server.forward-headers-strategy` behind a proxy) so ResourceSync URLs point to the public address |

## Database migrations

Flyway runs automatically at startup on `registryDataSource` (`db/migration`, V1.1–V6.0). `V5.0`
creates the core registry schema, the `vector` and `tablefunc` extensions and `resource_chunk` with
an HNSW index. `V6.0` adds the persisted full-text search vector. Per-type views are (re)created when
resource types are added, and missing ones at startup.

Upgrading a 4.x database: `V5.0` uses `CREATE ... IF NOT EXISTS` and `ADD COLUMN IF NOT EXISTS`, so
it aligns an existing schema. A test upgrade on a copy of production data is still advisable.

## Post-deployment verification

```bash
curl -s http://<host>:8080/actuator/health                                  # actuator default exposure
curl -s -H 'Accept: application/json' 'http://<host>:8080/resourceType/?from=0&to=10'  # registered resource types
curl -s -H 'Accept: application/xml' http://<host>:8080/resourcesync/       # ResourceSync URLs use the public base URL
```

Check the logs for `Caching the class path resource [onnx/all-MiniLM-L6-v2/model.onnx]`,
`Started RegistryService`, `Elastic Autoconfiguration enabled`, `Jms Autoconfiguration enabled`,
`Elasticsearch REST transport created for [...]`, and `Checking for index inconsistencies`.

## Backup, rollback and recovery

- Logical backup: `GET /dump/?schema=true&version=true` produces a ZIP. Restore with
  `POST /restore/` (multipart `datafile`).
- Database backups (`pg_dump`) remain the primary recovery mechanism. Elasticsearch can be rebuilt
  from PostgreSQL by restarting the service: `SearchIndexConsistencyService` recreates missing indices
  and indexes every resource that's missing from them. Documents that exist but are stale aren't
  refreshed.
- Rollback: redeploy the previous artefact. Flyway migrations aren't reversible, so restore the DB
  backup if a migration must be undone.
