package gr.uoa.di.madgik.registry.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;


@Configuration
@EnableJpaRepositories(basePackages = "gr.uoa.di.madgik.registry.dao")
@EnableTransactionManagement
@ComponentScan(value = {
        "gr.uoa.di.madgik.registry.*"
})

public class DatabaseConfiguration {

    public static final String TEST_RESOURCE_ID = "e98db949-f3e3-4d30-9894-7dd2e291fbef";
    public static final String TEST_MISSING_RESOURCE_ID = "not-existing-resource-id";

    @Value("classpath:dummy.sql")
    private Resource schemaScript;

    @Value("classpath:data.sql")
    private Resource dataScript;


    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource dataSource) {
        final DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator());
        return initializer;
    }

    private DatabasePopulator databasePopulator() {
        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(schemaScript);
        populator.addScript(dataScript);
        return populator;
    }
}
