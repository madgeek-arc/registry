# Registry

A generic, schema-driven resource store for Spring Boot. Applications register **resource types**
(JSON/XML Schema plus index-field definitions), then create, version, validate, search and export
**resources** of those types. Resources are persisted in PostgreSQL (with pgvector embeddings and
full-text search), optionally indexed in Elasticsearch, and changes can be announced over JMS.

## Role

Provides the persistence, versioning, validation and search layer that applications build on. They
embed `registry-starter-service` (and optionally the Elasticsearch and JMS starters) as libraries, or
talk to a remote registry through `registry-starter-client`.

This line (5.0.0) requires Spring Boot 4 and Java 25. The 4.3.x line is maintained on `master`.

## Securing the REST API

The registry has no authentication or authorisation. Any application that embeds
`registry-starter-service` registers the registry REST controllers by default, including bulk export
(`/dump/`), bulk import (`/restore/`) and deletes, and **is responsible for guarding those paths** in
its own security configuration. An application that only uses the registry in-process sets
`registry.rest.enabled=false`. See [docs/security.md](docs/security.md).

## Modules

| Module | Role |
|---|---|
| `registry-bom` | Bill of materials pinning all registry module versions |
| `registry-core-api` | Domain (`Resource`, `ResourceType`, `Schema`, `Version`, index fields), service interfaces, JMS payloads, listener contracts |
| `registry-core-resource-sync` | ResourceSync (ANSI/NISO Z39.99) XML domain model |
| `registry-core-helper` | Generic typed-resource helpers (`GenericResourceManager`, `ResourceCRUDService`, `ParserPool`) for applications built on the registry |
| `registry-core-rest` | REST controllers for resources, resource types, search, versions, schemas, dump/restore, ResourceSync, typed records (`/records`), and the fallback exception handler |
| `registry-starter-service` | Auto-configuration of the full service: JPA/PostgreSQL, Flyway, validation, indexing, SQL search (full-text and pgvector semantic/hybrid), versioning, Spring Batch dump/restore, embeddings, actuator |
| `registry-starter-elasticsearch` | Elasticsearch indexing and search (keyword, semantic, hybrid, highlighting, recommendations), DB↔index consistency check at startup |
| `registry-starter-jms` | Publishes resource create/update/delete events to ActiveMQ topics |
| `registry-starter-client` | REST client implementations of the service interfaces for remote registries |
| `registry-service` | Standalone Spring Boot application bundling the service, Elasticsearch and JMS starters and Swagger UI |

## Prerequisites

- JDK 25, Maven 3.9+ (no wrapper in this repository).
- PostgreSQL with the `vector` (pgvector) and `tablefunc` extensions available (created by Flyway
  migration `V5.0`), for example the `pgvector/pgvector:pg16` image.
- Optional: Elasticsearch 9.x, ActiveMQ (Classic) broker.
- Docker, to run the Testcontainers-based tests.

## Minimal local start (standalone service)

```bash
mvn -DskipTests -Ddependency-check.skip=true install
java -jar registry-service/target/RegistryService-5.0.0-SNAPSHOT.jar \
  --spring.config.additional-location=file:./application.properties
```

With only PostgreSQL available, add `--registry.elasticsearch.enabled=false --registry.jms.enabled=false`.
See [application-example.properties](application-example.properties) and
[docs/configuration.md](docs/configuration.md) for the settings. The OpenAPI document is served at
`/v3/api-docs` and the Swagger UI at `/swagger-ui.html`.

## Documentation

- [Architecture](docs/architecture.md) · [Integration](docs/integration.md) · [API](docs/api.md)
- [Configuration](docs/configuration.md) · [Example configuration](application-example.properties)
- [Deployment](docs/deployment.md) · [Operations](docs/operations.md) · [Troubleshooting](docs/troubleshooting.md)
- [Security](docs/security.md) · [Development](docs/development.md) · [Testing](docs/testing.md)
- [Contributing](CONTRIBUTING.md) · [Changelog](CHANGELOG.md)

## Licence

Apache License 2.0, see [LICENSE](LICENSE). Maintained by the madgeek-arc team
(<https://github.com/madgeek-arc/registry>).
