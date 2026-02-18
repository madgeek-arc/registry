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
     * @return the embedding vector - not l2 normalized
     */
    @Override
    public float[] embed(List<Segment> segments) {
        float weightSum = 0; // to normalize at the end
        float[] result = new float[VECTOR_SIZE];
        for (Segment segment : segments) {
            if (segment.getWeight() > 0 && !segment.getValues().isEmpty()) {
                weightSum += segment.getWeight();
                float[] pooledVector = new float[VECTOR_SIZE];
                for (String text : segment.getValues()) {
                    String embeddingText = "[%s]: %s".formatted(segment.getLabel(), text);
                    float[] embedding = embeddingModel.embed(embeddingText);
                    for (int i = 0; i < VECTOR_SIZE; i++) { // adds weighted embedding to pool
                        pooledVector[i] += (embedding[i] * segment.getWeight());
                    }
                }
                for (int i = 0; i < VECTOR_SIZE; i++) { // creates mean(pooledVector) and adds it to result
                    result[i] += (pooledVector[i] / segment.getValues().size());
                }
            }
        }
        for (int i = 0; i < VECTOR_SIZE; i++) { // scale down the values using the weightSum
            result[i] /= weightSum;
        }
        // It is possible to normalize the result and use dot product instead of cosine similarity.
        return result;
    }
}
