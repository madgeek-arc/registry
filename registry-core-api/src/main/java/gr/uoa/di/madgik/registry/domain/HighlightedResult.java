package gr.uoa.di.madgik.registry.domain;

import java.util.List;
import java.util.function.Function;

public class HighlightedResult<T> {
    private Float score;
    private T result;
    private List<Highlight> highlights;

    public HighlightedResult() {
    }

    public static <T> HighlightedResult<T> of(Float score, T result, List<Highlight> highlights) {
        HighlightedResult<T> hr = new HighlightedResult<>();
        hr.setScore(score);
        hr.setResult(result);
        hr.setHighlights(highlights);
        return hr;
    }

    public <U> HighlightedResult<U> map(Function<? super T, ? extends U> converter) {
        HighlightedResult<U> hr = new HighlightedResult<>();
        hr.setScore(this.score);
        hr.setResult(converter.apply(this.result));
        hr.setHighlights(this.highlights);
        return hr;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public List<Highlight> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<Highlight> highlights) {
        this.highlights = highlights;
    }
}
