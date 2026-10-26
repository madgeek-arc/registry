# Registry — Architecture

## Purpose and scope

**Does**

- Manages **resource types**: name, aliases, payload type (JSON or XML), schema (JSON Schema or XSD),
  and index-field definitions with JSONPath/XPath expressions, search capabilities and embedding
  weights. (`registry-core-api/.../domain/ResourceType.java`, `index/IndexField.java`)
- Stores **resources** (payloads of a type). It validates them against the type schema, extracts
  typed index fields into per-type tables, and keeps version history. Updates whose payload, format
  and type are unchanged are skipped. (`service/ResourceServiceImpl.java`,
  `validation/`, `index/`, `monitor/VersionMonitor.java`, `monitor/ResourceMonitor.java`)
- Searches resources in PostgreSQL: keyword search through per-type views (`<type>_view`, built with
  `tablefunc` `crosstab`) with a persisted full-text vector, and semantic/hybrid search over pgvector
  embeddings stored per resource chunk. When the Elasticsearch starter is present, Elasticsearch
  serves search instead, with keyword, semantic, hybrid, highlighted and recommendation queries.
 (`dao/ViewDaoImpl.java`, `service/DefaultSearchService.java`, `service/lexical/`,
  `service/ResourceChunkIndexService.java`, `elasticsearch/service/ElasticSearchService.java`)
- Publishes change events to JMS topics. (`registry-starter-jms`)
- Dumps and restores all data as a ZIP archive with Spring Batch, and exposes ResourceSync feeds.
 (`configuration/BackupRestoreConfig.java`, `controllers/RestoreController.java`,
  `controllers/ResourceSyncController.java`)
- Offers a typed REST API (`/records/{resourceType}/...`) that addresses resources by their primary
  key fields. (`controllers/TypedResourceController.java`)

**Does not**

- Authenticate or authorise callers. No module configures Spring Security. Host applications can
  leave the REST controllers out with `registry.rest.enabled=false`.
- Define domain-specific types. Applications supply resource-type JSON files (loaded at startup from
  `registry.resource-type-init.location`) or register them through the API.
  (`startup/ResourceTypeInit`)

## Technical stack

| Item | Value | Evidence |
|---|---|---|
| Java | 25 (`-parameters` enabled) | `pom.xml` |
| Spring Boot | 4.0.7 | `pom.xml` parent |
| Persistence | Hibernate 6.5.2 (pinned), Spring Data JPA, HikariCP 7, Flyway 11, PostgreSQL with pgvector | `pom.xml`, `registry-starter-service/pom.xml` |
| Batch | Spring Batch 6 | Spring Boot 4 BOM |
| Search | Elasticsearch Java API client 9.4.3 (`Rest5Client` transport), `cql-elasticsearch` | `registry-starter-elasticsearch/pom.xml` |
| Messaging | ActiveMQ client, Spring JMS with the Jackson 3 `JacksonJsonMessageConverter` | `registry-starter-jms` |
| Embeddings | Spring AI 1.1 transformers with the bundled `all-MiniLM-L6-v2` ONNX model, 384-dimension vectors | `RegistryEmbeddingModelDefaults`, `EmbeddingService.VECTOR_SIZE` |
| Validation | everit-json-schema 1.14.6, Saxon-HE 12 (XML/XPath) | `registry-starter-service/pom.xml` |
| JSON | Jackson 3 (`tools.jackson`) | POMs |
| API docs | springdoc-openapi 3.0.3 (api in `registry-core-rest`, UI in `registry-service`) | POMs |
| Logging | Logback through SLF4J (Spring Boot default) | resolved dependencies |
| Caching | Caffeine `CacheManager` (1,000 entries, 1 h after access) unless the host defines one | `RegistryServiceAutoConfiguration#cacheManager` |

## Main modules and responsibilities

| Module | Key packages / classes | Responsibility |
|---|---|---|
| `registry-bom` | — | Version alignment for consumers |
| `registry-core-api` | `domain`, `domain.index`, `domain.jms`, `service`, `monitor`, `exception` | Entities `Resource`, `ResourceType`, `Schema` (`SchemaDatabase` table), `Version` (`ResourceVersion` table), `IndexField`, `IndexedField`, `ResourceChunk`. Service interfaces. Listener interfaces `ResourceListener`/`ResourceTypeListener`. JMS payloads |
| `registry-core-resource-sync` | `resourcesync.domain` | ResourceSync XML model |
| `registry-core-helper` | `service.GenericResourceManager`, `ResourceCRUDService`, `ParserPool`, `FacetLabelService` | Typed-resource services for applications |
| `registry-core-rest` | `controllers.*`, `controllers.advice.GlobalExceptionHandler` | REST endpoints (see [api.md](api.md)) and the lowest-precedence RFC 7807 exception mapping |
| `registry-starter-service` | `autoconfigure` (`RegistryServiceAutoConfiguration`, `RegistryServiceComponentsConfiguration`, `RegistryRestControllersConfiguration`, `RegistryEmbeddingServiceAutoConfiguration`, `RegistryEmbeddingModelDefaults`, `JaxbAutoConfiguration`), `configuration` (`HibernateConfiguration`, `BatchConfig`, `BackupRestoreConfig`, `ServiceConfiguration`), `dao`, `service`, `index`, `validation`, `monitor`, `startup`, `backup`, `security` | Full in-process implementation |
| `registry-starter-elasticsearch` | `ElasticAutoConfiguration`, `ElasticOperationsService`, `ElasticSearchService` (`@Primary SearchService`), listeners, `SearchIndexConsistencyService` | Index maintenance and search. Enabled unless `registry.elasticsearch.enabled=false` |
| `registry-starter-jms` | `JmsAutoConfiguration`, `JmsProperties`, `JmsResourceListener` | Topic publishing. Enabled unless `registry.jms.enabled=false` |
| `registry-starter-client` | `RegistryClientAutoConfiguration`, `RegistryClientProperties`, `client.*` | `RestTemplate` implementations of `ResourceService`, `ResourceTypeService`, `SearchService`, `VersionService`, `DumpService`, `RestoreService`, `GenericResourceService` against `registry.base` |
| `registry-service` | `RegistryService` (`@SpringBootConfiguration` + `@EnableAutoConfiguration`, no component scan) | Standalone app = service + Elasticsearch + JMS starters + Swagger UI |

All beans come from the starters' auto-configurations (`META-INF/spring/...AutoConfiguration.imports`),
so the `registry.*.enabled` switches behave the same in `registry-service` and in host applications.

## Component diagram

Source: [diagrams/component-diagram.mmd](diagrams/component-diagram.mmd)

```mermaid
flowchart LR
    APP["Host application<br/>(embeds starters)"] -- "Java service API" --> SVC
    HTTP["HTTP clients /<br/>registry-starter-client"] -- "REST" --> REST

    subgraph REG[registry-starter-service + registry-core-rest]
        REST["Controllers (registry.rest.enabled)<br/>/resources /resourceType /search /records<br/>/version /dump /restore /resourcesync"] --> SVC
        SVC[ResourceService / ResourceTypeService<br/>validation, index mapping] --> DAO[DAOs + ViewDao]
        SVC -. "@Around" .-> MON[ResourceMonitor]
        MON --> VER[VersionMonitor]
        MON --> CHUNK[ResourceChunkIndexService]
        SRCH[DefaultSearchService<br/>full-text + pgvector] --> DAO
        BATCH[Spring Batch<br/>dump / restore] --> DAO
        EMB[WeightedSegmentEmbeddingService<br/>bundled ONNX model]
        CHUNK --> EMB
        SRCH --> EMB
    end

    subgraph ES[registry-starter-elasticsearch]
        ESL[ElasticResourceListener] --> EOPS[ElasticOperationsService]
        ESS[ElasticSearchService<br/>@Primary SearchService]
        SYNC[SearchIndexConsistencyService<br/>startup reindex]
    end

    subgraph JMS[registry-starter-jms]
        JMSL[JmsResourceListener]
    end

    MON --> ESL
    MON --> JMSL
    EOPS --> EMB
    ESS --> EMB
    DAO --> PG[(PostgreSQL + pgvector<br/>tables, chunks, per-type views)]
    CHUNK --> PG
    EOPS --> ELASTIC[(Elasticsearch 9.x)]
    ESS --> ELASTIC
    SYNC --> ELASTIC
    SYNC --> PG
    JMSL -- "topics prefix.type.create/update/delete" --> MQ[(ActiveMQ)]
```

## Main data flow: create/update a resource

Source: [diagrams/data-flow.mmd](diagrams/data-flow.mmd)

1. `ResourceServiceImpl` validates the payload against the type's JSON Schema or XSD, extracts index
   fields through the type's `IndexMapper`, and persists the `Resource` plus its `IndexedField` rows.

2. The `ResourceMonitor` aspect (`@Around` `ResourceService.addResource/updateResource/deleteResource/changeResourceType`
   and the `ResourceTypeService` methods) notifies **every** `ResourceListener` once the call returns:
   `VersionMonitor` (writes a `ResourceVersion`), `ResourceChunkIndexService` (embeds the resource
   into `resource_chunk` rows), `ElasticResourceListener` (indexes the document, including
   embeddings), and `JmsResourceListener` (publishes an event). An update whose stored payload,
   format and type are unchanged returns early, without notifying listeners.
3. **Listener exceptions are caught and logged**, so the caller still sees success even if versioning,
   chunk embedding, indexing or messaging failed. (`ResourceMonitor`)

Resource type creation also creates the PostgreSQL view `<type>_view` and the Elasticsearch index
mapping (`dense_vector` with 384 dims for embeddings). Type and index-field names must match
`^[a-zA-Z_][a-zA-Z0-9_]*$` before they are used as SQL identifiers. (`ViewDaoImpl`,
`ViewResourceTypeListener`, `ElasticOperationsService`)

## Startup behaviour

| Hook | Effect | Evidence |
|---|---|---|
| `RegistryEmbeddingModelDefaults` (environment post-processor) | Points Spring AI at the ONNX model and tokenizer packaged in the service starter, as lowest-precedence defaults | `META-INF/spring.factories` |
| Flyway | Applies `db/migration` V1.1–V6.0: Spring Batch tables and v5 migration, `tablefunc`, Hibernate 6 sequence migration, the 5.0 schema (core tables, `vector` extension, `resource_chunk` with an HNSW index), and the persisted full-text search vector | `registry-starter-service/src/main/resources/db/migration` |
| `ResourceTypeInit` (`ApplicationRunner`) | Loads `*.json` resource types from `registry.resource-type-init.location` (default `classpath*:resourceTypes`). With `registry.resource-type-init.refresh=true`, types whose content changed are updated | `startup/ResourceTypeInit` |
| `ResourceTypeViewInit` | Creates missing `<type>_view` views | `startup/ResourceTypeViewInit` |
| `EmbeddingWarmup` | Runs one embedding to load the model at startup | `startup/EmbeddingWarmup` |
| `SearchIndexConsistencyService#reindexOnInit` | After `ApplicationReadyEvent`, on a virtual thread: for each type, re-creates missing indices, indexes DB resources missing from Elasticsearch, and logs `ERROR` for index entries missing in the DB | `registry-starter-elasticsearch` |

## Data and state

| Store | Contents | Schema management | Evidence |
|---|---|---|---|
| PostgreSQL `registryDataSource` | `resource`, `resourcetype`, `schemadatabase`, `resourceversion`, `indexfield`, typed `*indexedfield` tables, `resource_chunk` (384-dim `vector`), per-type `<type>_view` views, Spring Batch tables | Flyway (`V5.0` creates the core schema). `registry.jpa.properties.hibernate.hbm2ddl.auto` may still be set by the host | migrations, `HibernateConfiguration` |
| Elasticsearch | One index per resource type (documents plus embedding vectors) | Created by `ElasticOperationsService` when types are added or found missing | `ElasticOperationsService`, `SearchIndexConsistencyService` |
| Filesystem (transient) | Dump ZIPs are written under the servlet context's real path before streaming | — | `DumpController` |

The registry owns its database. Host applications share it when they embed the starter (bean names
`registryDataSource`, `registryTransactionManager`, `entityManagerFactory`, all `@Primary`).
(`HibernateConfiguration`)

## Security

There's no authentication or authorisation, and the dump/restore endpoints export and overwrite
the whole store. See [security-model.md](security-model.md).

## Significant design decisions

- **Schema-driven, type-agnostic storage**: payloads stay opaque JSON/XML. Only declared index
  fields are extracted into typed tables and views.
- **Extensibility through listeners**: versioning, chunk embedding, indexing and messaging plug in
  as `ResourceListener` beans. They're best-effort: errors are logged, not propagated.
- **Search without Elasticsearch**: PostgreSQL alone serves keyword, semantic and hybrid search.
  Elasticsearch is optional.
- **Embedded starters as the primary distribution**: the same code runs in-process in applications
  or behind `registry-service`/`registry-starter-client`.
- **Self-healing index**: Elasticsearch is reconciled from PostgreSQL at every startup.
  (`SearchIndexConsistencyService`)
