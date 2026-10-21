# Registry — Security

## Authentication and authorisation

**None in this repository.** No module configures Spring Security. `registry-core-helper` depends on
`spring-security-core` only as a library.

As a result:

- `registry-service` exposes every endpoint, including `DELETE /resources/{id}`,
  `DELETE /resourceType/{name}`, `GET /dump/` (full export) and `POST /restore/` (bulk import),
  without authentication.
- In applications that embed `registry-starter-service`, the same controllers are component-scanned
  into the application, and there is no property to turn them off.
  (`RegistryServiceAutoConfiguration` scans `gr.uoa.di.madgik.registry.controllers`)

## Responsibility of the consuming application

Guarding the registry's REST paths is the responsibility of the application that embeds the service
starter, or of whoever deploys `registry-service`. Cover every registry path in the application's
security configuration, or restrict access at the network or gateway level. The paths are
`/resources/**`, `/resourceType/**`, `/search/**`, `/version/**`, `/schemaService/**`, `/dump/`, `/restore/` and `/resourcesync/**` (relative to the context path). A rule that only lists some
of them leaves the rest open.

The `version/5.0.0` line adds `registry.rest.enabled=false` to
leave the controllers out.

## Input validation

- Payloads are validated against the resource type's JSON Schema (everit) or XSD before storage.
 (`ResourceValidator`)
- Resource-type creation changes the database schema (views), so restrict it to trusted callers.
- Restore accepts uploaded ZIP files (multipart). Only trusted operators should be able to call it.


## Secrets and transport

- Secrets: DB, Elasticsearch and JMS passwords. Inject them through the environment or a secret
  store (see [configuration.md](configuration.md#secrets)). The repository ships no runtime property
  files.
- Elasticsearch defaults to `http://` when no scheme is given. Use `https://` URIs in production.
 (`ElasticAutoConfiguration#restHighLevelClient`)
- JMS defaults to unauthenticated `tcp://localhost:61616`. Use credentials and TLS (`ssl://`) brokers
  in production. (`JmsProperties`)

## CORS / CSRF

Not configured by the registry. Hosts decide.

## Personal data

Resource payloads may contain personal data, which is stored in PostgreSQL, Elasticsearch, version
history, JMS messages (full resources, including the previous version on update) and dump archives.
Erasure therefore has to cover all of these. `VersionService.updateVersion` exists to rewrite
history (changelog `4.3.0`).

Test configuration logs SQL at `DEBUG`. Avoid `org.hibernate.SQL=DEBUG` with bind-parameter logging
in production.

## Dependency scanning

OWASP dependency-check runs in the default build (`failBuildOnCVSS=7.6`, suppressions in
`dependency-check-suppressions.xml`).
