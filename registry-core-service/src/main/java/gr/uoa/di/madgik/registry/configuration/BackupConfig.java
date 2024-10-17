package gr.uoa.di.madgik.registry.configuration;

import gr.uoa.di.madgik.registry.backup.dump.*;
import gr.uoa.di.madgik.registry.backup.restore.RestoreResourceReaderStep;
import gr.uoa.di.madgik.registry.backup.restore.RestoreResourceTypeStep;
import gr.uoa.di.madgik.registry.backup.restore.RestoreResourceWriterStep;
import gr.uoa.di.madgik.registry.domain.Resource;
import jakarta.transaction.Transactional;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Callable;

@Configuration(proxyBeanMethods = false)
public class BackupConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final int chunkSize;

    public BackupConfig(JobRepository jobRepository,
                        @Qualifier("registryTransactionManager") PlatformTransactionManager transactionManager,
                        @Value("${batch.chunkSize:10}") int chunkSize) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.chunkSize = chunkSize;
    }

    @Bean
    @JobScope
    Step resourceTypeStep(RestoreResourceTypeStep restoreResourceTypeStep) {
        return new StepBuilder("resourceTypeStep", jobRepository)
                .tasklet(restoreResourceTypeStep, transactionManager)
                .build();
    }

    @Bean
    @JobScope
    Callable<TaskExecutor> threadPoolExecutor(@Value("#{jobParameters['resourceType']}") String resourceType) {
        return () -> {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2 * Runtime.getRuntime().availableProcessors() - 1);
            executor.setMaxPoolSize(2 * Runtime.getRuntime().availableProcessors() - 1);
            executor.setThreadNamePrefix(resourceType + "_job_pool_");
            executor.initialize();
            return executor;
        };
    }

    @Bean
    @JobScope
    Step resourcesStep(
            RestoreResourceReaderStep reader,
            RestoreResourceWriterStep writer,
            Callable<TaskExecutor> threadPoolExecutor
    ) throws Exception {
        return new StepBuilder("resourcesChunkStep", jobRepository)
                .<Resource, Resource>chunk(chunkSize, transactionManager)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .taskExecutor(threadPoolExecutor.call())
                .build();
    }

    @Bean
    @Transactional
    Step resourcesDumpStep(DumpResourceReader reader, DumpResourceWriterStep writer) {
        return new StepBuilder("resourcesDumpChunkStep", jobRepository)
                .<Resource, Resource>chunk(chunkSize, transactionManager)
                .reader(reader).faultTolerant()
                .retryPolicy(new AlwaysRetryPolicy())
                .writer(writer)
                .faultTolerant()
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .build();
    }

    @Bean
    Step resourcesTypeDumpStep(DumpResourceTypeStep step) {
        return new StepBuilder("resourcesTypeDumpStep", jobRepository)
                .tasklet(step, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    Step resourceDump(Step resourcesDumpStep,
                      DumpResourcePartitioner resourcePartitioner,
                      Callable<TaskExecutor> threadPoolExecutor
    ) throws Exception {
        return new StepBuilder("resourcePartitioner", jobRepository)
                .partitioner("resourcePartitioner", resourcePartitioner)
                .step(resourcesDumpStep)
                .taskExecutor(threadPoolExecutor.call())
                .build();
    }

    @Bean
    @JobScope
    Step resourceTypeDumpPartitioner(DumpResourceTypePartitioner partitioner, Step resourceDump) {
        return new StepBuilder("resourcesType", jobRepository)
                .partitioner("resourceTypePartitioner", partitioner)
                .step(resourceDump)
                .build();
    }

    @Bean
    Job restoreJob(Step resourcesStep, Step resourceTypeStep) {
        return new JobBuilder("restore", jobRepository)
                .start(resourceTypeStep)
                .next(resourcesStep)
                .build();
    }


    @Bean
    Job dumpJob(Step resourcesTypeDumpStep, Step resourceTypeDumpPartitioner) {
        return new JobBuilder("dump", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(resourcesTypeDumpStep)
                .next(resourceTypeDumpPartitioner)
                .build();
    }

}
