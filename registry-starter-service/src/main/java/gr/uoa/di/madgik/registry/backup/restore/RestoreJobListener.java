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
