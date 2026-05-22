package gr.uoa.di.madgik.registry.startup;

import gr.uoa.di.madgik.registry.service.EmbeddingService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingWarmup implements ApplicationRunner {

    private final EmbeddingService embeddingService;

    public EmbeddingWarmup(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        embeddingService.embed("warmup");
    }
}
