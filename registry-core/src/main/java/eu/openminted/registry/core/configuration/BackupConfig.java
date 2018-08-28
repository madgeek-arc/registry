package eu.openminted.registry.core.configuration;

import eu.openminted.registry.core.backup.dump.DumpResourceReader;
import eu.openminted.registry.core.backup.dump.DumpResourceTypePartitioner;
import eu.openminted.registry.core.backup.dump.DumpResourceTypeStep;
import eu.openminted.registry.core.backup.dump.DumpResourceWriterStep;
import eu.openminted.registry.core.backup.restore.RestoreResourceReaderStep;
import eu.openminted.registry.core.backup.restore.RestoreResourceTypeStep;
import eu.openminted.registry.core.backup.restore.RestoreResourceWriterStep;
import eu.openminted.registry.core.domain.Resource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Callable;

@Configuration()
public class BackupConfig {

    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    @Value("${batch.chunkSize:10}")
    private int chunkSize;

    @Bean
    @JobScope
    Step resourceTypeStep(RestoreResourceTypeStep restoreResourceTypeStep) {
        return steps.get("resourceTypeStep")
                .tasklet(restoreResourceTypeStep)
                .build();
    }

    @Bean
    @JobScope
    Callable<TaskExecutor> threadPoolExecutor(@Value("#{jobParameters['resourceType']}") String resourceType) {
        return () -> {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2* Runtime.getRuntime().availableProcessors()-1);
            executor.setMaxPoolSize(2* Runtime.getRuntime().availableProcessors()-1);
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
        return steps.get("resourcesChunkStep")
                .<Resource,Resource>chunk(chunkSize)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .taskExecutor(threadPoolExecutor.call())
                .build();
    }

    @Bean
    Step resourcesDumpStep(DumpResourceReader reader, DumpResourceWriterStep writer) {

        return steps.get("resourcesDumpChunkStep")
                .<Resource,Resource>chunk(chunkSize)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .taskExecutor(new SyncTaskExecutor()) //this has to by SyncTaskExecutor due to hibernate session
                .build();
    }

    @Bean
    Step resourcesTypeDumpStep(DumpResourceTypeStep step) {
        return steps.get("resourcesTypeDumpStep")
                .tasklet(step)
                .build();
    }

    @Bean
    @JobScope
    Step resourceTypeDumpPartitioner(
            Step resourcesDumpStep,
            DumpResourceTypePartitioner partitioner,
            Callable<TaskExecutor> threadPoolExecutor
    ) throws Exception {
        return steps.get("resourcesType")
                .partitioner("resourceTypePartitioner", partitioner)
                .step(resourcesDumpStep)
                .taskExecutor(threadPoolExecutor.call())
                .build();
    }

    @Bean
    Job restoreJob(Step resourcesStep, Step resourceTypeStep) {
        return jobs.get("restore")
                .incrementer(new RunIdIncrementer())
                .start(resourceTypeStep)
                .next(resourcesStep)
                .build();
    }



    @Bean
    Job dumpJob(Step resourcesTypeDumpStep, Step resourceTypeDumpPartitioner) {
        return jobs.get("dump")
                .incrementer(new RunIdIncrementer())
                .start(resourcesTypeDumpStep)
                .next(resourceTypeDumpPartitioner)
                .build();
    }

}
