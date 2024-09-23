package gr.uoa.di.madgik.registry.backup.restore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RestoreJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(RestoreJobListener.class);

    private final Map<String, JobExecution> registeredJobs;

    public RestoreJobListener() {
        registeredJobs = new ConcurrentHashMap<>();
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String name = jobExecution.getJobParameters().getString("resourceType");
        logger.debug("Job started " + name);
    }

    @Override
    synchronized public void afterJob(JobExecution jobExecution) {
        String name = jobExecution.getJobParameters().getString("resourceType");
        logger.debug("Job finished " + name);
        notify();
    }

    public void registerJob(JobExecution job) {
        registeredJobs.put(job.getJobParameters().getString("resourceType"), job);
    }

    public Collection<JobExecution> getJobs() {
        return registeredJobs.values();
    }

    synchronized public List<BatchStatus> waitResults() throws InterruptedException {
        while (registeredJobs.values().stream().anyMatch(JobExecution::isRunning)) {
            logger.info("Awaiting");
            wait();
        }
        return registeredJobs.values().stream().map(JobExecution::getStatus).collect(Collectors.toList());
    }
}
