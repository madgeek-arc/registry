package gr.uoa.di.madgik.registry.domain;

import java.util.List;

public class HighlightedResult<T> {
    private T result;
    private List<Highlight> highlights;

    public HighlightedResult() {
    }

    public static <T> HighlightedResult<T> of(T result, List<Highlight> highlights) {
        HighlightedResult<T> hr = new HighlightedResult<>();
        hr.setResult(result);
        hr.setHighlights(highlights);
        return hr;
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
