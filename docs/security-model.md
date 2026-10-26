# Registry — Security model

## Authentication and authorisation

**None in this repository.** No module configures Spring Security. `registry-core-helper` depends on
`spring-security-core` only as a library, and `registry-starter-service` declares
`spring-boot-starter-security` as an optional dependency, used only for the encoded-slash firewall
option.

As a result:

- `registry-service` exposes every endpoint, including `DELETE /resources/{id}`,
  `DELETE /resourceType/{name}`, `DELETE /records/{resourceType}/{id}`, `GET /dump/` (full export)
  and `POST /restore/` (bulk import), without authentication.
- In applications that embed `registry-starter-service`, the same controllers are registered by
  default. (`RegistryRestControllersConfiguration`)

## Responsibility of the consuming application

Guarding the registry's REST paths is the responsibility of the application that embeds the service
starter, or of whoever deploys `registry-service`. The registry logs this at startup (`INFO Registry
REST controllers registered without authentication ...`). Either:

- leave the controllers out with `registry.rest.enabled=false` when the registry is only used
  in-process (`RestControllersDisabledIntegrationTest`), or
- cover every registry path in the application's security configuration, or restrict access at the
  network or gateway level. The paths are `/records/**`, `/resources/**`, `/resourceType/**`,
  `/search/**`, `/version/**`, `/schemaService/**`, `/dump/`, `/restore/` and `/resourcesync/**`
  (relative to the context path). A rule that only lists some of them leaves the rest open.

## Input validation

- Payloads are validated against the resource type's JSON Schema (everit) or XSD before storage.
 (`ResourceSchemaValidator`)
- Native SQL uses bound parameters for values (for example `VersionDaoImpl`). Resource-type and
  index-field names, which become SQL identifiers in views and search queries, must match
  `^[a-zA-Z_][a-zA-Z0-9_]*$`, and column names are quoted. (`ViewDaoImpl`,
  `DefaultSearchService`, `SqlIdentifiers`)
- Resource-type creation still changes the database schema (views), so it should be limited to
  trusted callers.
- Restore accepts uploaded ZIP files (multipart). Only trusted operators should be able to call it.

- `%2F` in IDs is rejected by default. `registry.rest.allow-encoded-slash=true` relaxes Tomcat and,
  when present, the Spring Security firewall for `/records/**` and the configured paths only.
 (`EncodedSlashHttpFirewall` tests)

## Error responses

Unexpected exceptions return 500 with a generic message. Their details are only logged. Registry
exceptions return their message in the problem detail. (`GlobalExceptionHandler`)

## Secrets and transport

- Secrets: DB, Elasticsearch and JMS passwords. Inject them through the environment or a secret
  store (see [configuration.md](configuration.md#secrets)). The jars ship no runtime property files.
- Elasticsearch defaults to `http://` when no scheme is given. Use `https://` URIs in production.
  Credentials are sent to every configured node. (`ElasticAutoConfiguration#restClient`)
- JMS defaults to unauthenticated `tcp://localhost:61616`. Use credentials and TLS (`ssl://`) brokers
  in production. (`JmsProperties`)

## CORS / CSRF

Not configured by the registry. Hosts decide.

## Personal data

Resource payloads may contain personal data, which is stored in PostgreSQL (including
`resource_chunk` text and embeddings), Elasticsearch, version history, JMS messages (full resources,
including the previous version on update) and dump archives. Erasure therefore has to cover all of
these. `VersionService.updateVersion` exists to rewrite history.

Test configuration logs SQL at `DEBUG`. Avoid `org.hibernate.SQL=DEBUG` with bind-parameter logging
in production.

## Dependency scanning

OWASP dependency-check runs in the default build (`failBuildOnCVSS=7.6`, suppressions in
`dependency-check-suppressions.xml`).
