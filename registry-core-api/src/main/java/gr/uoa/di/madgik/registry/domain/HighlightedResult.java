/*
 * Copyright 2025-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
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
