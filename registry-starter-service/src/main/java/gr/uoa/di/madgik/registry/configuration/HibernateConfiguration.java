package gr.uoa.di.madgik.registry.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties({DataSourceProperties.class, JpaProperties.class})
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
    @FlywayDataSource
    @ConfigurationProperties("app.datasource.configuration")
    public DataSource registryDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("registryDataSource") DataSource registryDataSource,
                                                                       @Qualifier("registryJpaProperties") JpaProperties registryJpaProperties) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(registryDataSource);
        emf.setPackagesToScan("gr.uoa.di.madgik.registry.domain", "gr.uoa.di.madgik.registry.domain.index");
        emf.setPersistenceUnitName("registryEntityManager");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(registryJpaProperties.getProperties());

        return emf;
    }

    @Bean("registryTransactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

}
