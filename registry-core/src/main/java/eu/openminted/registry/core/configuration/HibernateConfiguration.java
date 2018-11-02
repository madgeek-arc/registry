package eu.openminted.registry.core.configuration;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan({ "eu.openminted.registry.core.dao" })
@PropertySource(value = { "classpath:application.properties", "classpath:registry.properties"} )
@EnableAspectJAutoProxy
@EnableCaching
public class HibernateConfiguration {

	private static Logger logger = LogManager.getLogger(HibernateConfiguration.class);

	@Autowired
	private Environment environment;

	@Bean
	public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer(){
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setLocations(new ClassPathResource("application.properties"),new ClassPathResource("registry.properties"));
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		return ppc;
	}

	@Bean(initMethod = "migrate")
    public Flyway flyway(){
	    Flyway flyway = new Flyway();
	    flyway.setBaselineOnMigrate(true);
	    flyway.setLocations("classpath:migrations/");
	    flyway.setDataSource(dataSource());
	    flyway.setOutOfOrder(true);
        return flyway;
    }

    @Bean
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("eu.openminted.registry.core.domain", "eu.openminted.registry.core.domain.index");
//        em.setPersistenceUnitName("MY_PERSISTENCE");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(hibernateProperties());

        return em;
    }

    @Bean
	@Autowired
	public EntityManager entityManager(EntityManagerFactory entityManagerFactory){
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.setFlushMode(FlushModeType.AUTO);
		return entityManager;
	}

    @Bean
	@Autowired
    public PlatformTransactionManager transactionManager( EntityManagerFactory emf ){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

	@Bean(destroyMethod="close")
	public DataSource dataSource(){
		ComboPooledDataSource dataSource = new ComboPooledDataSource();
////		DriverManagerDataSource dataSource = new DriverManagerDataSource();
//		dataSource.setDriverClassName(environment.getRequiredProperty("jdbc.driverClassName"));
//		dataSource.setUrl(environment.getRequiredProperty("jdbc.url")+"?autoReconnect=true");
//		dataSource.setUsername(environment.getRequiredProperty("jdbc.username"));
//		dataSource.setPassword(environment.getRequiredProperty("jdbc.password"));
		try {
			dataSource.setDriverClass(environment.getRequiredProperty("jdbc.driverClassName"));
		} catch (PropertyVetoException e) {
			logger.error(e.getMessage(),e);
		}
		dataSource.setJdbcUrl(environment.getRequiredProperty("jdbc.url"));
		dataSource.setUser(environment.getRequiredProperty("jdbc.username"));
		dataSource.setPassword(environment.getRequiredProperty("jdbc.password"));
		dataSource.setAcquireIncrement(2);
		dataSource.setMinPoolSize(2);
		dataSource.setMaxPoolSize(20);
		dataSource.setMaxIdleTime(60);//seconds
		dataSource.setPreferredTestQuery("SELECT 1");
		dataSource.setDebugUnreturnedConnectionStackTraces(true);
		dataSource.setTestConnectionOnCheckout(true);
		dataSource.setTestConnectionOnCheckin(true);
		dataSource.setIdleConnectionTestPeriod(10);
		dataSource.setInitialPoolSize(2);
		dataSource.setMaxStatements(0);
		dataSource.setMaxStatementsPerConnection(0);
		dataSource.setPrivilegeSpawnedThreads(true);
        try {
            dataSource.setContextClassLoaderSource("library");
        } catch (PropertyVetoException e) {
            logger.error("Unable to create context class load for c3p0",e);
        }

        return dataSource;
	}

	private Properties hibernateProperties() {
		Properties properties = new Properties();
		properties.put("hibernate.dialect", environment.getRequiredProperty("hibernate.dialect"));
		properties.put("hibernate.show_sql", environment.getRequiredProperty("hibernate.show_sql"));
		properties.put("hibernate.format_sql", environment.getRequiredProperty("hibernate.format_sql"));
		properties.put("hibernate.hbm2ddl.auto", environment.getRequiredProperty("hibernate.hbm2ddl.auto"));
		properties.put("hibernate.enable_lazy_load_no_trans","true");
		return properties;
	}

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("resourceTypes","resourceTypesIndexFields");
    }

}