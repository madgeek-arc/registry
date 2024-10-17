package gr.uoa.di.madgik.registry.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement(proxyTargetClass = true)
public class HibernateConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(HibernateConfiguration.class);

    @Bean("registryJpaProperties")
    @Primary
    @ConfigurationProperties("registry.jpa")
    JpaProperties registryJpaProperties() {
        return new JpaProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("registry.datasource")
    public DataSourceProperties registryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("registryDataSource")
    @Primary
    @ConfigurationProperties("registry.datasource.configuration")
    public DataSource registryDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                       @Qualifier("registryDataSource") DataSource registryDataSource,
                                                                       @Qualifier("registryJpaProperties") JpaProperties registryJpaProperties) {
        LocalContainerEntityManagerFactoryBean em = builder
                .dataSource(registryDataSource)
                .packages("gr.uoa.di.madgik.registry.domain", "gr.uoa.di.madgik.registry.domain.index")
                .persistenceUnit("registryEntityManager")
                .build();

        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaPropertyMap(registryJpaProperties.getProperties());

        return em;
    }

    @Bean("registryTransactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }


    @Bean(initMethod = "migrate")
    public Flyway flyway(@Qualifier("registryDataSource") DataSource registryDataSource) {
        return Flyway.configure()
                .dataSource(registryDataSource)
                .locations("classpath:migrations")
                .load();
    }

}
