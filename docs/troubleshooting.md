# Registry — Troubleshooting

| Problem | Possible cause | What to check | Resolution |
|---|---|---|---|
| Startup fails: `Could not resolve placeholder 'registry.datasource.url'` (or username/password) | Required DB keys missing | Effective configuration | Set all three `registry.datasource.*` keys (username/password may be empty) |
| Flyway fails on `CREATE EXTENSION vector` or `tablefunc` | pgvector not installed, or the DB user lacks the privilege | Flyway logs | Use a PostgreSQL image with pgvector (for example `pgvector/pgvector:pg16`), or pre-create the extensions as a superuser |
| ResourceSync URLs show an internal host | `registry.host` unset, so the request URL is used | `GET /resourcesync/` | Set `registry.host`, or enable `server.forward-headers-strategy` behind a proxy |
| Registry endpoints return 404 in a host application | `registry.rest.enabled=false` (or `registry.rest.typed-resources-controller.enabled=false` for `/records`) | Effective configuration | Remove the switch if the endpoints are wanted |
| Search results or embeddings missing or stale after writes | A listener failed | `ERROR Error notifying listener` | Fix the cause (Elasticsearch, model), then restart (startup reindex) or update the resources again |
| `Database is missing the following resources` at startup | Index entries deleted or rolled back in the DB | `SearchIndexConsistencyService` log | Delete the stale index entries, or recreate the index |
| No JMS events received | `registry.jms.enabled=false`, wrong `host`/`prefix`, or broker down | `Jms Autoconfiguration enabled`, `ActiveMQConnection Factory created for ...` | Fix the broker settings. Subscribe to `<prefix>.<type>.<create\|update\|delete>` topics |
| Resource-type creation not seen by subscribers | Not published by design | `WARN JMS is NOT notified ...` | Use another mechanism for type changes |
| Elasticsearch connection refused | Wrong `registry.elasticsearch.uris` | Startup `Elasticsearch REST transport created for [...]` | List every node as `scheme://host[:port]` (default port 9200, or 443 for `https`) |
| Semantic search fails or returns nothing; `No EmbeddingService found` | No embedding model, for example because the Spring AI transformers starter is excluded | Startup `WARN` | Keep the transformers starter, or provide an `EmbeddingModel`/`EmbeddingService` bean |
| Startup uses a different embedding model than expected | `spring.ai.embedding.transformer.*` set by the application | Effective configuration | Remove the overrides to use the packaged model |
| Registry client calls fail with `ResourceAccessException` after 60 s | `registry.client.read-timeout` reached (large dump or restore, slow registry) | Client logs | Raise `registry.client.read-timeout` |
| `404` on `/resourceType` or `/version` | `Accept` header isn't exactly `application/json` | Request headers | Send the header (mappings match on it) |
| Host app's own JPA/DataSource beans ignored | Registry beans are `@Primary` | Bean definitions | Qualify the host's beans explicitly |
| Host app's own `@ControllerAdvice` loses to the registry's | Both advices handle the same exception and neither has an explicit order | Exception handlers | Give the host advice a higher-precedence `@Order` |
| Build fails in `dependency-check` | NVD download, or CVSS ≥ 7.6 finding | Maven output | Use `-Ddependency-check.skip=true` locally. Fix or suppress the CVE |
| Tests fail with `Could not find a valid Docker environment` | Docker not available | `docker ps` | Start Docker. The tests use Testcontainers |
