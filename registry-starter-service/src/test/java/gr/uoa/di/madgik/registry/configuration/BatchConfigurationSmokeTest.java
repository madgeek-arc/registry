package gr.uoa.di.madgik.registry.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.transaction.Transactional;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
class BatchConfigurationSmokeTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    @Qualifier("dumpJob")
    private Job dumpJob;

    @Autowired
    @Qualifier("restoreJob")
    private Job restoreJob;

    @Autowired
    private JobLauncher jobLauncher;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void batchBeans_are_available_in_context() {
        Assertions.assertNotNull(jobRepository);
        Assertions.assertEquals("dump", dumpJob.getName());
        Assertions.assertEquals("restore", restoreJob.getName());
    }

    @Test
    void jobLauncher_is_resolvable_with_request_scope() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        Assertions.assertNotNull(jobLauncher);
    }
}
