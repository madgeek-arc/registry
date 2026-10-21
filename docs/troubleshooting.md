# Registry — Troubleshooting

| Problem | Possible cause | What to check | Resolution |
|---|---|---|---|
| Startup fails: `Could not resolve placeholder 'registry.datasource.url'` (or username/password) | Required DB keys missing | Effective configuration | Set all three `registry.datasource.*` keys (username/password may be empty) |
| Startup fails: required key `registry.host` not found | `ResourceSyncServiceImpl` needs it | Configuration | Set `registry.host` to the public base URL |
| Flyway fails on `CREATE EXTENSION tablefunc` | DB user lacks privilege, or the extension isn't installed | Flyway logs | Pre-create the extension as a superuser, or grant the privilege |
| Search results missing or stale after writes | Elasticsearch listener failed | `ERROR Error notifying listener` | Fix Elasticsearch connectivity, then restart (startup reindex) |
| `Database is missing the following resources` at startup | Index entries deleted or rolled back in the DB | `IndexDbSync` log | Delete the stale index entries, or recreate the index |
| No JMS events received | `registry.jms.enabled=false`, wrong `host`/`prefix`, or broker down | `Jms Autoconfiguration enabled`, `ActiveMQConnection Factory created for ...` | Fix the broker settings. Subscribe to `<prefix>.<type>.<create\|update\|delete>` topics |
| Resource-type creation not seen by subscribers | Not published by design | `WARN JMS is NOT notified ...` | Use another mechanism for type changes |
| Elasticsearch connection to the wrong host or port | Only the first `registry.elasticsearch.uris` entry is used. The URI must contain an explicit port | Startup `Elasticsearch RestHighLevelClient created for ...` | Put the intended node first, with `scheme://host:port` |
| First search is slow, or startup downloads files | Embedding model initialisation and download | `Embedding model warmed up` / warm-up `WARN` | Pre-cache the model. Allow outbound HTTPS access to GitHub |
| `406`/`404` on `/resourceType` or `/version` | Missing `Accept: application/json` header | Request headers | Send the header (mappings match on it) |
| Host app's own JPA/DataSource beans ignored | Registry beans are `@Primary` | Bean definitions | Qualify the host's beans explicitly |
| Build fails in `dependency-check` | NVD download, or CVSS ≥ 7.6 finding | Maven output | Use `-Ddependency-check.skip=true` locally. Fix or suppress the CVE |
