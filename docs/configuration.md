# Registry — Configuration

All settings are Spring Boot properties supplied by the host application or the `registry-service`
runtime. The jars ship no `application.properties` or `application.yaml`. The embedding-model
defaults are contributed by an environment post-processor at the lowest precedence, so any value set
by the application wins. A copyable example is in
[`../application-example.properties`](../application-example.properties). Standard Spring Boot
precedence applies.

## Database (`registry-starter-service`)

Bound in `configuration/HibernateConfiguration.java`.

| Property | Required | Default | Description |
|---|---|---|---|
| `registry.datasource.url` | **Yes** | — | JDBC URL of a PostgreSQL database with the `vector` and `tablefunc` extensions available |
| `registry.datasource.username` | **Yes** (may be empty) | — | Startup fails if the key is missing |
| `registry.datasource.password` | **Yes** (may be empty) | — | **Secret** |
| `registry.datasource.driver-class-name` | No | detected from URL | |
| `registry.datasource.configuration.*` | No | Hikari defaults | Pool settings for `registryDataSource` (`maximum-pool-size`, `connection-timeout`, ...) |
| `registry.jpa.properties.*` | No | — | Raw Hibernate properties, for example `hibernate.show_sql`. The schema comes from Flyway, so `hibernate.hbm2ddl.auto` can stay unset |
| `spring.flyway.enabled` | No | Spring Boot default `true` | Flyway runs on `registryDataSource` (`@FlywayDataSource`) and creates the schema |

## Service behaviour

| Property | Required | Default | Description | Evidence |
|---|---|---|---|---|
| `registry.host` | No | current request URL | Public base URL used in ResourceSync documents (trailing `/` removed). When unset, the scheme, host, port and context path of the current request are used | `ResourceSyncServiceImpl` |
| `registry.rest.enabled` | No | `true` | Registers the registry REST controllers. They have no authentication, so when enabled the application must guard their paths. `false` leaves them out; the services and the exception handler stay | `RegistryRestControllersConfiguration` |
| `registry.rest.typed-resources-controller.enabled` | No | `true` | `false` leaves out `/records/**` only | same |
| `registry.rest.allow-encoded-slash` | No | `false` | Accept `%2F` in `/records/**` IDs (Tomcat passthrough plus a Spring Security firewall relaxation when Spring Security is present) | `RegistryServiceAutoConfiguration`, `EncodedSlashHttpFirewallConfiguration` |
| `registry.rest.encoded-slash-paths` | No | — | Extra path prefixes, besides `records`, where the Spring Security firewall accepts `%2F` | `EncodedSlashHttpFirewallConfiguration` |
| `registry.resource-type-init.location` | No | `classpath*:resourceTypes` | Folder (or single `.json`) of resource types loaded at startup | `ResourceTypeInit` |
| `registry.resource-type-init.refresh` | No | `false` | Update stored types whose file content changed | `ResourceTypeInit` |
| `registry.sql.search.semantic-min-score` | No | `0.3` | Minimum cosine similarity for PostgreSQL semantic search | `SqlSearchProperties` |
| `registry.sql.search.lexical-strategy` | No | `full-text` | Lexical matching strategy for PostgreSQL search | `SqlSearchProperties` |
| `registry.sql.search.highlight.*` | No | `payload-context-chars=80`, `payload-max-fragments=5`, `semantic-max-chars=220` | Highlight sizes for PostgreSQL search | `SqlSearchProperties` |
| `registry.jaxb.include-packages` | For XML payloads | empty | Packages scanned for `@XmlRootElement` classes | `JaxbProperties` |
| `batch.chunkSize` | No | `10` | Spring Batch chunk size for dump/restore | `BackupRestoreConfig` |
| `spring.ai.embedding.transformer.onnx.modelUri`, `.onnx.modelOutputName`, `.tokenizer.uri`, `spring.ai.model.embedding` | No | the packaged `all-MiniLM-L6-v2` model and tokenizer (`classpath:/onnx/...`), `token_embeddings`, `transformers` | Override to use another 384-dimension model | `RegistryEmbeddingModelDefaults` |

## Elasticsearch (`registry-starter-elasticsearch`)

Bound by `RegistryElasticsearchProperties` (`registry.elasticsearch`).

| Property | Required | Default | Description |
|---|---|---|---|
| `registry.elasticsearch.enabled` | No | `true` | `false` disables the starter. PostgreSQL search is then used |
| `registry.elasticsearch.uris` | When enabled | — | One client node per entry. `http://` is assumed without a scheme. The port defaults to `9200` (`http`) or `443` (`https`) |
| `registry.elasticsearch.username` / `.password` | No | — | Basic auth for every node, applied when both are set. **Secret** |
| `registry.elasticsearch.aggregation.top-hits-size` | No | `100` | Top hits per facet bucket |
| `registry.elasticsearch.aggregation.bucket-size` | No | `100` | Facet bucket size |
| `registry.elasticsearch.index.max-result-window` | No | `10000` | Paging limit |
| `registry.elasticsearch.index.bulk-batch-size` | No | `100` | Bulk indexing batch size |
| `registry.elasticsearch.search.highlight.fragment-size` / `.number-of-fragments` | No | `400` / `5` | Highlighting |

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
| `registry.client.connect-timeout` | No | `10s` | Connect timeout of the client `RestTemplate` |
| `registry.client.read-timeout` | No | `60s` | Read timeout. Raise it for large dumps and restores |

A `RestTemplate` bean defined by the application replaces the client's own, and its timeouts then
apply instead. (`RegistryClientAutoConfigurationTest`)

## Embedding the service starter in a host

`HibernateConfiguration` registers `registryDataSource`, `entityManagerFactory` and
`registryTransactionManager` as **`@Primary`**, and `BatchConfig` enables Spring Batch on them.
A host that also has its own persistence has to give its beans distinct names and qualify them
explicitly.

## Environments

| Environment | Notes |
|---|---|
| Tests | `registry-starter-service/src/test/resources/application-test.properties` with a `pgvector/pgvector:pg16` Testcontainers database, `hbm2ddl.auto=create-drop`, Flyway disabled, SQL logging |
| Production | Set per deployment through environment variables or an external configuration. Not stored in the repository |

## Secrets

`registry.datasource.password`, `registry.elasticsearch.password` and `registry.jms.password`.
Supply them through environment variables (for example `REGISTRY_DATASOURCE_PASSWORD`, through
Spring Boot relaxed binding) or a secret store. Never commit them.
