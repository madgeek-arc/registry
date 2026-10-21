# Registry — Development

## Tools

| Tool | Version | Evidence |
|---|---|---|
| JDK | 21 | `pom.xml` `java.version` |
| Maven | 3.9+ (no wrapper) | repository root |
| Docker (optional) | for local PostgreSQL, Elasticsearch 7.x, ActiveMQ | — |

## Commands

```bash
mvn -B -Ddependency-check.skip=true clean verify         # build + tests + javadoc
mvn -B -DskipTests -Ddependency-check.skip=true install  # install SNAPSHOTs for local consumers
mvn -pl registry-service -am -DskipTests -Ddependency-check.skip=true package
java -jar registry-service/target/RegistryService-<version>.jar --spring.config.additional-location=file:./application.properties
```

Verified: `mvn verify -Ddependency-check.skip=true` passes at `5a05baf` (46 tests).

## Local dependencies

- PostgreSQL with `tablefunc` available (the standard `postgres` images include contrib).
- Disable optional infrastructure while developing:
  `registry.elasticsearch.enabled=false`, `registry.jms.enabled=false`.
- Put resource-type JSON files under `src/main/resources/resourceTypes/` of the host, or point
  `registry.resource-type-init.location` at a folder.

## Working with consumers

Applications built on the registry (and on libraries built on top of it) consume the installed
SNAPSHOT. After changing this repository, run `mvn install -DskipTests` before rebuilding them.

## Conventions

- Conventional Commits (`feat:`, `fix(search):`, `refactor:`, `build:` ...). The release plugin uses
  `chore(release):`.
- Apache 2.0 header on every Java file. `registry-core-resource-sync` has its own `LICENSE_HEADER`.
  No header-check plugin is configured.
- Maven resource filtering is enabled for `src/main/resources` (excluding `application*` files), so
  `${...}` tokens in resources are substituted at build time. (`pom.xml`)
- New `ResourceListener`/`ResourceTypeListener` beans are notified automatically by
  `ResourceMonitor`. They must not rely on exceptions reaching callers.
