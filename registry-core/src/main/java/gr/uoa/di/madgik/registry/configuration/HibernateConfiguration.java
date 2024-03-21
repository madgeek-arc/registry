package gr.uoa.di.madgik.registry.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableAspectJAutoProxy
@EnableCaching
@PropertySource(value = {"classpath:application.properties"})
@EnableTransactionManagement(proxyTargetClass = true)
public class HibernateConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(HibernateConfiguration.class);

    @Autowired
    private Environment environment;

    @Bean(name = "registryEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean registryEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("gr.uoa.di.madgik.registry.domain", "gr.uoa.di.madgik.registry.domain.index");

        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(hibernateProperties());

        return em;
    }

    @Bean("registryEntityManager")
    @Autowired
    public EntityManager registryEntityManager(@Qualifier("registryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.createEntityManager();
    }

    @Bean("registryTransactionManager")
    @Primary
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(registryEntityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    public HikariConfig hikariConfig() {
        logger.info("Connecting to Database @ " + environment.getRequiredProperty("jdbc.url"));
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("RegistryCP");
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setDriverClassName(environment.getRequiredProperty("jdbc.driverClassName"));
        hikariConfig.setMaximumPoolSize(20);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(120000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setJdbcUrl(environment.getRequiredProperty("jdbc.url"));
        hikariConfig.setUsername(environment.getRequiredProperty("jdbc.username"));
        hikariConfig.setPassword(environment.getRequiredProperty("jdbc.password"));
        hikariConfig.addDataSourceProperty("cachePreStmts", "true"); // Enable Prepared Statement caching
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "25"); // How many PS cache, default: 25
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true"); // If supported use PS server-side
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true"); // Enable setAutoCommit
        hikariConfig.addDataSourceProperty("useLocalTransactionState", "true"); // Enable commit/rollbacks


        return hikariConfig;
    }


    @Bean("registryDataSource")
    @Primary
    public DataSource dataSource() {
        return new HikariDataSource(hikariConfig());
    }


    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", environment.getRequiredProperty("hibernate.dialect"));
        properties.put("hibernate.show_sql", environment.getRequiredProperty("hibernate.show_sql"));
        properties.put("hibernate.format_sql", environment.getRequiredProperty("hibernate.format_sql"));
        properties.put("hibernate.hbm2ddl.auto", environment.getRequiredProperty("hibernate.hbm2ddl.auto"));
        properties.put("hibernate.enable_lazy_load_no_trans", "true");
        properties.put("hibernate.allow_update_outside_transaction", "true");

        return properties;
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("resourceTypes", "resourceTypesIndexFields");
    }


    @PostConstruct
    public void flywayMigration() {
        Flyway flyway = Flyway.configure().dataSource(dataSource()).locations("classpath:migrations").load();
        try {
            flyway.baseline();
        } catch (FlywayException ex) {
            logger.warn("Flyway exception on baseline", ex);
        }
        flyway.migrate();
    }

}
