# Registry — Development

## Tools

| Tool | Version | Evidence |
|---|---|---|
| JDK | 25 | `pom.xml` `java.version` |
| Maven | 3.9+ (no wrapper) | repository root |
| Docker | Required for the tests (Testcontainers); useful for local PostgreSQL (pgvector), Elasticsearch 9.x and ActiveMQ | — |

## Commands

```bash
mvn -B -Ddependency-check.skip=true clean verify         # build + tests + javadoc
mvn -B -DskipTests -Ddependency-check.skip=true install  # install SNAPSHOTs for local consumers
mvn -pl registry-service -am -DskipTests -Ddependency-check.skip=true package
java -jar registry-service/target/RegistryService-<version>.jar --spring.config.additional-location=file:./application.properties
```

Verified: `mvn clean install -Ddependency-check.skip=true` passes on `fix/report-issues` (301 tests).

## Local dependencies

- PostgreSQL with pgvector and `tablefunc`, for example
  `docker run -e POSTGRES_PASSWORD=... -p 5432:5432 pgvector/pgvector:pg16`.
- Disable optional infrastructure while developing:
  `registry.elasticsearch.enabled=false`, `registry.jms.enabled=false`.
- Put resource-type JSON files under `src/main/resources/resourceTypes/` of the host, or point
  `registry.resource-type-init.location` at a folder.

## Working with consumers

Applications built on the registry (and on libraries built on top of it) consume the installed
SNAPSHOT. After changing this repository, run `mvn install -DskipTests` before rebuilding them. This
line needs consumers on Spring Boot 4 and Java 25.

## Conventions

- Conventional Commits (`feat:`, `fix(search):`, `refactor:`, `build:` ...). The release plugin uses
  `chore(release):`.
- Apache 2.0 header on every Java file. `registry-core-resource-sync` has its own `LICENSE_HEADER`.
  No header-check plugin is configured.
- Maven resource filtering is enabled for `application*.yaml` and `application*.properties` under
  `src/main/resources`, so `${...}` tokens there are substituted at build time. (`pom.xml`)
- Starters contribute beans through auto-configuration only. Component scans live in plain
  `@Configuration` classes imported by the auto-configurations. Nested `@Configuration` classes need
  their own conditions, because a host's component scan can pick them up directly.
- New `ResourceListener`/`ResourceTypeListener` beans are notified automatically by
  `ResourceMonitor`. They must not rely on exceptions reaching callers.
