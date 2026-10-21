# Registry — Architecture

## Purpose and scope

**Does**

- Manages **resource types**: name, aliases, payload type (JSON or XML), schema (JSON Schema or XSD),
  and index-field definitions with JSONPath/XPath expressions. (`registry-core-api/.../domain/ResourceType.java`, `IndexField.java`)
- Stores **resources** (payloads of a type). It validates them against the type schema, extracts
  typed index fields into per-type tables, and keeps version history.
  (`registry-starter-service/.../service/ResourceServiceImpl.java`, `validation/ResourceValidator.java`,
  `index/JSONFieldParser.java`, `index/XMLFieldParser.java`, `monitor/VersionMonitor.java`)
- Searches resources through PostgreSQL views (`<type>_view`, built with `tablefunc` `crosstab`), or,
  when the Elasticsearch starter is present, through Elasticsearch with keyword and embedding-vector
  scoring and recommendations. (`dao/ViewDaoImpl.java`, `service/DefaultSearchService.java`,
  `elasticsearch/service/ElasticSearchService.java`)
- Publishes change events to JMS topics. (`registry-starter-jms`)
- Dumps and restores all data as a ZIP archive with Spring Batch, and exposes ResourceSync feeds.
 (`configuration/BackupRestoreConfig.java`, `controllers/RestoreController.java`,
  `controllers/ResourceSyncController.java`)

**Does not**

- Authenticate or authorise callers. There's no Spring Security configuration in any module.

- Define domain-specific types. Applications supply resource-type JSON files (loaded at startup from
  `registry.resource-type-init.location`) or register them through the API.
  (`ResourceTypeInit`)

## Technical stack

| Item | Value | Evidence |
|---|---|---|
| Java | 21 (`-parameters` enabled) | `pom.xml` |
| Spring Boot | 3.5.15 | `pom.xml` parent |
| Persistence | Hibernate 6.5.2, Spring Data JPA, HikariCP, Flyway (PostgreSQL) | `pom.xml`, `registry-starter-service/pom.xml` |
| Batch | Spring Batch 5.1.2 | `pom.xml` |
| Search | Elasticsearch High Level REST Client 7.17.24, `cql-elasticsearch` | `registry-starter-elasticsearch/pom.xml` |
| Messaging | ActiveMQ client (BOM 6.2.1), Spring JMS | `registry-starter-jms/pom.xml` |
| Embeddings | Spring AI 1.1.2 `spring-ai-starter-model-transformers`, 384-dimension vectors | `registry-starter-service/pom.xml`, `EmbeddingService.VECTOR_SIZE` |
| Validation | everit-json-schema 1.14.6, Saxon-HE (XML/XPath) | `registry-starter-service/pom.xml` |
| API docs | springdoc-openapi 2.8.16 (api in `registry-core-rest`, UI in `registry-service`) | POMs |
| Logging | Log4j2 through `log4j-slf4j-impl` | `pom.xml` |

## Main modules and responsibilities

| Module | Key packages / classes | Responsibility |
|---|---|---|
| `registry-core-api` | `domain`, `domain.index`, `domain.jms`, `service`, `monitor`, `exception` | Entities `Resource`, `ResourceType`, `Schema` (`SchemaDatabase` table), `Version` (`ResourceVersion` table), `IndexField`, `IndexedField`. Service interfaces. Listener interfaces `ResourceListener`/`ResourceTypeListener`. JMS payloads |
| `registry-core-resource-sync` | `domain` | JAXB-style ResourceSync model |
| `registry-core-helper` | `service.AbstractGenericService` and helpers | Base classes for typed services in applications |
| `registry-core-rest` | `controllers.*` | REST endpoints (see [api.md](api.md)) |
| `registry-starter-service` | `autoconfigure.RegistryServiceAutoConfiguration` (component scan of `gr.uoa.di.madgik.registry`), `configuration` (`HibernateConfiguration`, `BatchConfig`, `BackupRestoreConfig`, `ServiceConfiguration`), `dao`, `service`, `index`, `validation`, `monitor` (`ResourceMonitor` aspect, `VersionMonitor`), `dump`, `restore`, `backup`, `ResourceTypeInit` | Full in-process implementation |
| `registry-starter-elasticsearch` | `ElasticAutoConfiguration`, `ElasticOperationsService`, `ElasticSearchService` (`@Primary SearchService`), listeners, `IndexDbSync` | Index maintenance and search. Enabled unless `registry.elasticsearch.enabled=false` |
| `registry-starter-jms` | `JmsAutoConfiguration`, `JmsProperties`, `JmsResourceListener` | Topic publishing. Enabled unless `registry.jms.enabled=false` |
| `registry-starter-client` | `RegistryClientAutoConfiguration`, `client.*` | `RestTemplate` implementations of `ResourceService`, `ResourceTypeService`, `SearchService`, `VersionService`, `DumpService`, `RestoreService` against `registry.base` |
| `registry-service` | `RegistryService` (`@SpringBootApplication`) | Standalone app = service + Elasticsearch + JMS starters + Swagger UI |

## Component diagram

Source: [diagrams/component-diagram.mmd](diagrams/component-diagram.mmd)

```mermaid
flowchart LR
    APP["Host application<br/>(embeds starters)"] -- "Java service API" --> SVC
    HTTP["HTTP clients /<br/>registry-starter-client"] -- "REST" --> REST

    subgraph REG[registry-starter-service + registry-core-rest]
        REST[Controllers<br/>/resources /resourceType /search<br/>/version /dump /restore /resourcesync] --> SVC
        SVC[ResourceService / ResourceTypeService<br/>validation, index mapping] --> DAO[DAOs + ViewDao]
        SVC -. "@Around" .-> MON[ResourceMonitor]
        MON --> VER[VersionMonitor]
        SRCH[DefaultSearchService] --> DAO
        BATCH[Spring Batch<br/>dump / restore] --> DAO
        EMB[WeightingEmbeddingService<br/>Spring AI transformers]
    end

    subgraph ES[registry-starter-elasticsearch]
        ESL[ElasticResourceListener] --> EOPS[ElasticOperationsService]
        ESS[ElasticSearchService<br/>@Primary SearchService]
        SYNC[IndexDbSync<br/>startup reindex]
    end

    subgraph JMS[registry-starter-jms]
        JMSL[JmsResourceListener]
    end

    MON --> ESL
    MON --> JMSL
    EOPS --> EMB
    ESS --> EMB
    DAO --> PG[(PostgreSQL<br/>tables + per-type views)]
    EOPS --> ELASTIC[(Elasticsearch 7.x)]
    ESS --> ELASTIC
    SYNC --> ELASTIC
    SYNC --> PG
    JMSL -- "topics prefix.type.create/update/delete" --> MQ[(ActiveMQ)]
```

## Main data flow: create/update a resource

Source: [diagrams/data-flow.mmd](diagrams/data-flow.mmd)

1. `ResourceServiceImpl` (class-level `@Transactional`) validates the payload against the type's
   JSON Schema or XSD, extracts index fields through the type's `IndexMapper`, and persists the
   `Resource` plus its `IndexedField` rows.
2. The `ResourceMonitor` aspect (`@Around` `ResourceService.addResource/updateResource/deleteResource/changeResourceType`
   and the `ResourceTypeService` methods) notifies **every** `ResourceListener` once the call returns:
   `VersionMonitor` (writes a `ResourceVersion`), `ElasticResourceListener` (indexes the document,
   including embeddings), and `JmsResourceListener` (publishes an event).
3. **Listener exceptions are caught and logged**, so the caller still sees success even if versioning,
   indexing or messaging failed. (`ResourceMonitor`)

Resource type creation also (re)creates the PostgreSQL view `<type>_view` and the Elasticsearch
index mapping (`dense_vector` with 384 dims for `embedding`). (`ViewDaoImpl`,
`ElasticOperationsService`)

## Startup behaviour

| Hook | Effect | Evidence |
|---|---|---|
| Flyway | Applies `db/migration` V1.1–V4.0: Spring Batch tables, Batch v5 migration, `CREATE EXTENSION tablefunc`, Hibernate 6 sequence migration | `registry-starter-service/src/main/resources/db/migration` |
| `ResourceTypeInit#addResourceTypes` | Loads `*.json` resource types from `registry.resource-type-init.location` (default `classpath:resourceTypes`) | `ResourceTypeInit` |
| `IndexDbSync#reindexOnInit` | On a virtual thread: for each type, indexes DB resources missing from Elasticsearch, creates missing indices, and logs `ERROR` for index entries missing in the DB | `IndexDbSync` |
| `embeddingServiceWarmupRunner` | Runs one embedding to load the model at startup | `ElasticAutoConfiguration` |

## Data and state

| Store | Contents | Schema management | Evidence |
|---|---|---|---|
| PostgreSQL `registryDataSource` | `Resource`, `ResourceType`, `SchemaDatabase`, `ResourceVersion`, `IndexField`, typed `*IndexedField` tables, per-type `<type>_view` views, Spring Batch tables | Entities: Hibernate (`registry.jpa.properties.hibernate.hbm2ddl.auto`). Batch tables and extension: Flyway | `HibernateConfiguration`, entities, migrations |
| Elasticsearch | One index per resource type (documents plus `embedding` vector) | Created by `ElasticOperationsService` when types are added or found missing | `ElasticOperationsService`, `IndexDbSync` |
| Filesystem (transient) | Dump ZIPs are written under the servlet context's real path before streaming | — | `DumpController#dumpAll` |

The registry owns its database. Host applications share it when they embed the starter (bean names
`registryDataSource`, `registryTransactionManager`, `entityManagerFactory`, all `@Primary`).
(`HibernateConfiguration`)

## Security

There's no authentication or authorisation, and the dump/restore endpoints export and overwrite
the whole store. See [security.md](security.md).

## Significant design decisions

- **Schema-driven, type-agnostic storage**: payloads stay opaque JSON/XML. Only declared index
  fields are extracted into typed tables and views.
- **Extensibility through listeners**: indexing, versioning and messaging plug in as
  `ResourceListener` beans activated by starters. They're best-effort: errors are logged, not
  propagated.
- **Embedded starters as the primary distribution**: the same code runs in-process in applications
  or behind `registry-service`/`registry-starter-client`.
- **Self-healing index**: Elasticsearch is reconciled from PostgreSQL at every startup.
  (`IndexDbSync`)
