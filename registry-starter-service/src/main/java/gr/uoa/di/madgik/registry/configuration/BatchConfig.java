package gr.uoa.di.madgik.registry.configuration;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.JobRegistrySmartInitializingSingleton;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

@Configuration(proxyBeanMethods = false)
@EnableBatchProcessing(
        dataSourceRef = "registryDataSource",
        transactionManagerRef = "registryTransactionManager")
public class BatchConfig {

    @Bean
    @ConditionalOnMissingBean
    public JobRegistrySmartInitializingSingleton jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistrySmartInitializingSingleton postProcessor = new JobRegistrySmartInitializingSingleton();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(name = "jobLauncher")
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

}
