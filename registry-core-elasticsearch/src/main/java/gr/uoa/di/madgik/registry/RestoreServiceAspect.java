package gr.uoa.di.madgik.registry;

import gr.uoa.di.madgik.registry.domain.Resource;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RestoreServiceAspect {

    private static final Logger logger = LoggerFactory.getLogger(RestoreServiceAspect.class);

    private final IndexDbSync indexDbSync;

    public RestoreServiceAspect(IndexDbSync indexDbSync) {
        this.indexDbSync = indexDbSync;
    }

    /**
     * Index all Database {@link Resource resources} to Elastic after a restore.
     */
    @Async
    @After("execution(* gr.uoa.di.madgik.registry.service.RestoreService.restoreDataFromZip(..))")
    public void indexAfterRestore() {
        indexDbSync.reindex();
        logger.info("Indexing completed");
    }

}