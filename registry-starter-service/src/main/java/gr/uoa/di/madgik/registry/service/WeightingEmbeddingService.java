package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WeightingEmbeddingService implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(WeightingEmbeddingService.class);
    private final EmbeddingModel embeddingModel;

    public WeightingEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * Creates an embedding vector based on the provided information.
     * Generates a vector for each of the {@code segments} and synthesizes them using their weights to create the
     * embedding vector.
     *
     * @param segments a list of attributes, values and weights which will be used to create an embedding
     * @return the embedding vector
     */
    @Override
    public float[] embed(List<Segment> segments) {
        StringBuilder embeddingTextBuilder = new StringBuilder();
        for (Segment segment : segments) {
            if (segment.getWeight() > 0) {
                String fieldEmbedding = "weight: %f | %s: %s"
                        .formatted(
                                segment.getWeight(),
                                segment.getLabel(),
                                String.join(",", segment.getValues())
                        );
                embeddingTextBuilder.append(fieldEmbedding);
            }
        }
        return embeddingModel.embed(embeddingTextBuilder.toString());
    }
}
