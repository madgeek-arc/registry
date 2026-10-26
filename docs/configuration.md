# Registry — Configuration

All settings are Spring Boot properties supplied by the host application or the `registry-service`
runtime. The repository ships **no** runtime `application.properties`: the root POM excludes
`application*.properties|yml|yaml` from every jar's resources (`pom.xml` `<resources>`). A copyable
example is in [`../application-example.properties`](../application-example.properties). Standard
Spring Boot precedence applies.

## Database (`registry-starter-service`)

Bound in `configuration/HibernateConfiguration.java`.

| Property | Required | Default | Description |
|---|---|---|---|
| `registry.datasource.url` | **Yes** | — | JDBC URL (PostgreSQL in production; tests use HSQLDB with `sql.syntax_pgs=true`) |
| `registry.datasource.username` | **Yes** (may be empty) | — | Startup fails if the key is missing |
| `registry.datasource.password` | **Yes** (may be empty) | — | **Secret** |
| `registry.datasource.driver-class-name` | No | detected from URL | |
| `registry.datasource.configuration.*` | No | Hikari defaults | Pool settings for `registryDataSource` (`maximum-pool-size`, `connection-timeout`, ...) |
| `registry.jpa.properties.*` | Recommended | — | Raw Hibernate properties, for example `hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect`, `hibernate.hbm2ddl.auto` |
| `spring.flyway.enabled` | No | Spring Boot default `true` | Flyway runs on `registryDataSource` (`@FlywayDataSource`) |

## Service behaviour

| Property | Required | Default | Description | Evidence |
|---|---|---|---|---|
| `registry.host` | **Yes** | — | Public base URL used in ResourceSync documents. Startup fails without it | `ResourceSyncServiceImpl` (`getRequiredProperty`) |
| `registry.resource-type-init.location` | No | `classpath:resourceTypes` | Folder (or single `.json`) of resource types loaded at startup | `ResourceTypeInit` |
| `batch.chunkSize` | No | `10` | Spring Batch chunk size for dump/restore | `BackupRestoreConfig` |
| `spring.ai.embedding.transformer.*` | No | Spring AI defaults | Embedding model, tokenizer and cache location | `spring-ai-starter-model-transformers` |

## Elasticsearch (`registry-starter-elasticsearch`)

| Property | Required | Default | Description | Evidence |
|---|---|---|---|---|
| `registry.elasticsearch.enabled` | No | `true` | `false` disables the starter (DB search is then used) | `ElasticAutoConfiguration` |
| `registry.elasticsearch.uris` | When enabled | Spring Boot `ElasticsearchProperties` default | Only the **first** URI is used. `http://` is assumed when no scheme is given | `ElasticAutoConfiguration#restHighLevelClient` |
| `registry.elasticsearch.username` / `.password` | No | — | Basic auth. **Secret** | same |
| `elastic.index.max_result_window` | No | `10000` | Paging limit | `ElasticSearchService`, `ElasticOperationsService` |
| `elastic.aggregation.bucketSize` | No | `100` | Facet bucket size | `ElasticSearchService` |
| `elastic.aggregation.topHitsSize` | No | `100` | Top hits per bucket | `ElasticSearchService` |

## JMS (`registry-starter-jms`)

Bound by `JmsProperties` (`registry.jms`).

| Property | Required | Default | Description |
|---|---|---|---|
| `registry.jms.enabled` | No | `true` | `false` disables publishing |
| `registry.jms.host` | No | `tcp://localhost:61616` | Broker URL |
| `registry.jms.prefix` | No | `registry` | Topic prefix and connection ID prefix |
| `registry.jms.username` / `.password` | No | empty | Broker credentials. **Secret** |

## Client (`registry-starter-client`)

| Property | Required | Default | Description |
|---|---|---|---|
| `registry.base` | **Yes** | — | Base URL of the remote registry REST API |

## Embedding the service starter in a host

`HibernateConfiguration` registers `registryDataSource`, `entityManagerFactory` and
`registryTransactionManager` as **`@Primary`**, and `BatchConfig` enables Spring Batch on them.
A host that also has its own persistence has to give its beans distinct names and qualify them
explicitly.

## Environments

| Environment | Notes |
|---|---|
| Tests | `registry-starter-service/src/test/resources/application-test.properties`: in-memory HSQLDB (PostgreSQL syntax), `hbm2ddl.auto=create-drop`, Flyway enabled, SQL logging |
| Production | Set per deployment through environment variables or an external configuration. Not stored in the repository |

## Secrets

`registry.datasource.password`, `registry.elasticsearch.password` and `registry.jms.password`.
Supply them through environment variables (for example `REGISTRY_DATASOURCE_PASSWORD`, through
Spring Boot relaxed binding) or a secret store. Never commit them.
