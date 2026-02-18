package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.Segment;

import java.util.List;

public interface EmbeddingService {

    /**
     * Creates an embedding vector of the provided text.
     *
     * @param text the text to create the embedding for
     * @return the embedding vector
     */
    float[] embed(String text);

    /**
     * Creates an embedding vector based on the provided information.
     *
     * @param segments a list of attributes, values and weights which will be used to create an embedding
     * @return the embedding vector
     */
    float[] embed(List<Segment> segments);
}
