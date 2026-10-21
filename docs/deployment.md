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
| JDK 21 | Yes | |
| Maven 3.9+ | Build | No wrapper in the repository |
| PostgreSQL | Yes | The DB user must be allowed to `CREATE EXTENSION tablefunc` (Flyway `V3.0`), or the extension must be pre-created. |
| Elasticsearch 7.x | If `registry-starter-elasticsearch` is on the classpath and enabled | High-level REST client 7.17.24, created without API compatibility mode, so use a 7.x server. Elastic supports 8.x servers for this client only with compatibility mode enabled |
| ActiveMQ Classic | If `registry-starter-jms` is on the classpath and enabled | Defaults to `tcp://localhost:61616` |
| Embedding model | When search embeddings are used | Spring AI 1.1.2 transformers downloads the ONNX model and tokenizer (`all-MiniLM-L6-v2`) from the Spring AI GitHub repository when the embedding model bean starts, and caches them under `java.io.tmpdir`. In offline environments, pre-populate the cache or point `spring.ai.embedding.transformer.*` at local resources |

## Build

```bash
mvn -B -Ddependency-check.skip=true clean install      # all modules + tests + javadoc jars
mvn -B dependency-check:check                          # OWASP scan; fails at CVSS >= 7.6
```

| Artefact | Path |
|---|---|
| Starters/libraries | `gr.uoa.di.madgik:registry-*:<version>` (deployed to the madgik repository with `mvn deploy`) |
| Standalone service | `registry-service/target/RegistryService-<version>.jar` (Spring Boot executable, `Start-Class gr.uoa.di.madgik.registry.RegistryService`) |

Releases: `mvn release:prepare release:perform` creates tag `v<version>` and commits prefixed
`chore(release):`. (`pom.xml` `maven-release-plugin`)

## Running `registry-service`

```bash
java -jar registry-service/target/RegistryService-<version>.jar \
  --spring.config.additional-location=file:./application.properties
```

| Aspect | Value |
|---|---|
| Port | `8080` (Spring Boot default, override with `server.port`) |
| Volumes | Writable servlet temp/real path for dump ZIPs. Model cache for embeddings (under `java.io.tmpdir`) |
| Network | PostgreSQL. Elasticsearch HTTP. ActiveMQ OpenWire. Outbound HTTPS to GitHub for the embedding model unless cached |
| Order | PostgreSQL → Elasticsearch/ActiveMQ → registry → consumers |

## Database migrations

Flyway runs automatically at startup on `registryDataSource` (`db/migration`, V1.1–V4.0). Entity
tables are created or updated by Hibernate according to `registry.jpa.properties.hibernate.hbm2ddl.auto`.
Per-type views are (re)created when resource types are added.

Upgrading across Hibernate 5→6 relies on `V4.0__Migrate_to_hibernate_v6.sql` (it renames
`hibernate_sequence` to `indexedfield_seq` with increment 50).

## Post-deployment verification

```bash
curl -s http://<host>:8080/actuator/health                              # actuator default exposure
curl -s -H 'Accept: application/json' http://<host>:8080/resourceType/  # registered resource types
```

Check the logs for `Elastic Autoconfiguration enabled`, `Jms Autoconfiguration enabled`,
`Embedding model warmed up`, and `Index is consistent with Database` (DEBUG) or
`Reindexing N missing resources`.

## Backup, rollback and recovery

- Logical backup: `GET /dump/?schema=true&version=true` produces a ZIP. Restore with
  `POST /restore/` (multipart `datafile`).
- Database backups (`pg_dump`) remain the primary recovery mechanism. Elasticsearch can be rebuilt
  from PostgreSQL by restarting the service: `IndexDbSync` recreates missing indices and indexes every
  resource that's missing from them. Documents that exist but are stale aren't refreshed.
- Rollback: redeploy the previous artefact. Flyway migrations aren't reversible, so restore the DB
  backup if a migration must be undone.
