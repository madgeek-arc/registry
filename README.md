# Registry

A generic, schema-driven resource store for Spring Boot. Applications register **resource types**
(JSON/XML Schema plus index-field definitions), then create, version, validate, search and export
**resources** of those types. Resources are persisted in PostgreSQL, optionally indexed in
Elasticsearch (with semantic embeddings), and changes can be announced over JMS.

## Role

Provides the persistence, versioning, validation and search layer that applications build on. They
embed `registry-starter-service` (and optionally the Elasticsearch and JMS starters) as libraries, or
talk to a remote registry through `registry-starter-client`.

## Securing the REST API

The registry has no authentication or authorisation, and every application that embeds
`registry-starter-service` gets the registry REST controllers, including bulk export (`/dump/`), bulk
import (`/restore/`) and deletes. **The consuming application is responsible for guarding those
paths** in its own security configuration. See [docs/security-model.md](docs/security-model.md).

## Version lines

These docs describe `master` (4.3.x). `version/5.0.0` is the Spring Boot 4 line.

## Modules

| Module | Role |
|---|---|
| `registry-core-api` | Domain (`Resource`, `ResourceType`, `Schema`, `Version`, index fields), service interfaces, JMS payloads, listener contracts |
| `registry-core-resource-sync` | ResourceSync (ANSI/NISO Z39.99) XML domain model |
| `registry-core-helper` | Generic typed-service helpers (`AbstractGenericService`) for applications built on the registry |
| `registry-core-rest` | REST controllers for resources, resource types, search, versions, schemas, dump/restore, ResourceSync |
| `registry-starter-service` | Auto-configuration of the full service: JPA/PostgreSQL, Flyway, validation, indexing, DB search, versioning, Spring Batch dump/restore, embeddings, actuator |
| `registry-starter-elasticsearch` | Elasticsearch indexing and search (keyword + vector), DB↔index consistency check at startup |
| `registry-starter-jms` | Publishes resource create/update/delete events to ActiveMQ topics |
| `registry-starter-client` | REST client implementations of the service interfaces for remote registries |
| `registry-service` | Standalone Spring Boot application bundling the service, Elasticsearch and JMS starters and Swagger UI |

## Prerequisites

- JDK 21, Maven 3.9+ (no wrapper in this repository).
- PostgreSQL with the `tablefunc` extension available (created by Flyway migration `V3.0`).
- Optional: Elasticsearch 7.x, ActiveMQ (Classic) broker.

## Minimal local start (standalone service)

```bash
mvn -DskipTests -Ddependency-check.skip=true install
java -jar registry-service/target/RegistryService-4.3.2-SNAPSHOT.jar \
  --spring.config.additional-location=file:./application.properties
```

See [application-example.properties](application-example.properties) and
[docs/configuration.md](docs/configuration.md) for the required settings. The OpenAPI document is
served at `/v3/api-docs` and the Swagger UI at `/swagger-ui.html`.

## Documentation

- [Architecture](docs/architecture.md) · [Integration](docs/integration.md) · [API](docs/api.md)
- [Configuration](docs/configuration.md) · [Example configuration](application-example.properties)
- [Deployment](docs/deployment.md) · [Operations](docs/operations.md) · [Troubleshooting](docs/troubleshooting.md)
- [Security model](docs/security-model.md) · [Security policy](SECURITY.md) · [Development](docs/development.md) · [Testing](docs/testing.md)
- [Contributing](CONTRIBUTING.md) · [Changelog](CHANGELOG.md)

## Licence

Apache License 2.0, see [LICENSE](LICENSE). Maintained by the madgeek-arc team
(<https://github.com/madgeek-arc/registry>).
