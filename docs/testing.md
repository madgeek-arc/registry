# Registry — Testing

All automated tests are in `registry-starter-service/src/test/java`.

| Class | Scope |
|---|---|
| `dao/ResourceDaoImplTest`, `ResourceTypeDaoImplTest`, `SchemaDaoImplTest`, `IndexFieldDaoImplTest`, `IndexedFieldDaoImplTest` | DAO behaviour against the database |
| `service/ResourceServiceImplTest` | Resource lifecycle, validation, indexing |
| `service/ResourceSyncServiceImplTest` | ResourceSync generation |
| `configuration/DatabaseConfiguration` | Test database wiring |

Test configuration: `src/test/resources/application-test.properties` (HSQLDB in-memory with
PostgreSQL syntax, `hbm2ddl.auto=create-drop`, Flyway enabled), with seed data in `data.sql` and
`dummy.sql`.

## Running

```bash
mvn -B -Ddependency-check.skip=true test
mvn -B -Ddependency-check.skip=true -pl registry-starter-service test -Dtest=ResourceServiceImplTest
```

Result at `5a05baf`: 46 tests, 0 failures, 0 errors. No external services are needed.

## Not covered

- Elasticsearch starter (indexing, search, `IndexDbSync`), JMS starter, client starter, REST controllers,
  Spring Batch dump/restore and the embedding service. There are no tests in those modules.

- Behaviour on real PostgreSQL (`tablefunc` views). HSQLDB is used.
- No coverage tool and no CI configuration in the repository.
