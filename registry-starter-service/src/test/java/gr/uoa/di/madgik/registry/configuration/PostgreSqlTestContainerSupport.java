package gr.uoa.di.madgik.registry.configuration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;

@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class PostgreSqlTestContainerSupport {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("registry")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("registry.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("registry.datasource.username", POSTGRES::getUsername);
        registry.add("registry.datasource.password", POSTGRES::getPassword);
        registry.add("registry.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }
}
