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

import gr.uoa.di.madgik.registry.backup.dump.*;
import gr.uoa.di.madgik.registry.backup.restore.RestoreResourceReaderStep;
import gr.uoa.di.madgik.registry.backup.restore.RestoreResourceTypeStep;
import gr.uoa.di.madgik.registry.backup.restore.RestoreResourceWriterStep;
import gr.uoa.di.madgik.registry.domain.Resource;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Callable;

@Configuration(proxyBeanMethods = false)
public class BackupRestoreConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final int chunkSize;

    public BackupRestoreConfig(JobRepository jobRepository,
                               @Qualifier("registryTransactionManager") PlatformTransactionManager transactionManager,
                               @Value("${batch.chunkSize:10}") int chunkSize) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.chunkSize = chunkSize;
    }

    @Bean
    @ConditionalOnMissingBean(name = "resourceTypeStep")
    @JobScope
    Step resourceTypeStep(RestoreResourceTypeStep restoreResourceTypeStep) {
        return new StepBuilder("resourceTypeStep", jobRepository)
                .tasklet(restoreResourceTypeStep, transactionManager)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "threadPoolExecutor")
    Callable<TaskExecutor> threadPoolExecutor() {
        return () -> {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2 * Runtime.getRuntime().availableProcessors() - 1);
            executor.setMaxPoolSize(2 * Runtime.getRuntime().availableProcessors() - 1);
            executor.setThreadNamePrefix("job-pool-");
            executor.initialize();
            return executor;
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "resourcesStep")
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
    @ConditionalOnMissingBean(name = "resourcesDumpStep")
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
    @ConditionalOnMissingBean(name = "resourcesTypeDumpStep")
    Step resourcesTypeDumpStep(DumpResourceTypeStep step) {
        return new StepBuilder("resourcesTypeDumpStep", jobRepository)
                .tasklet(step, transactionManager)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "resourceDump")
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
    @ConditionalOnMissingBean(name = "resourceTypeDumpPartitioner")
    @JobScope
    Step resourceTypeDumpPartitioner(DumpResourceTypePartitioner partitioner, Step resourceDump) {
        return new StepBuilder("resourcesType", jobRepository)
                .partitioner("resourceTypePartitioner", partitioner)
                .step(resourceDump)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "restoreJob")
    Job restoreJob(Step resourcesStep, Step resourceTypeStep) {
        return new JobBuilder("restore", jobRepository)
                .start(resourceTypeStep)
                .next(resourcesStep)
                .build();
    }


    @Bean
    @ConditionalOnMissingBean(name = "dumpJob")
    Job dumpJob(Step resourcesTypeDumpStep, Step resourceTypeDumpPartitioner) {
        return new JobBuilder("dump", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(resourcesTypeDumpStep)
                .next(resourceTypeDumpPartitioner)
                .build();
    }

}
