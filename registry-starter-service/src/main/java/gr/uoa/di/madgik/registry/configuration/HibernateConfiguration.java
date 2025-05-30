/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * <p>The unused arguments with the {@code @Value} and {@code @NotEmpty} annotations
     * enforce the existence of the registry datasource properties.
     * Their purpose is to override the default Spring DataSource error for missing values.</p>
     */
    @Bean("registryDataSourceProperties")
    @Primary
    @ConfigurationProperties("registry.datasource")
    public DataSourceProperties registryDataSourceProperties(@Value("${registry.datasource.url}")
                                                             String db,
                                                             @Value("${registry.datasource.username}")
                                                             String user,
                                                             @Value("${registry.datasource.password}")
                                                             String password) {
        return new DataSourceProperties();
    }

    @Bean("registryDataSource")
    @Primary
    @FlywayDataSource
    @ConfigurationProperties("registry.datasource.configuration")
    public HikariDataSource registryDataSource(
            @Qualifier("registryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("registryDataSource") HikariDataSource registryDataSource,
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
