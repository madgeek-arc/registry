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

import java.util.function.Function;

public class ScoredResult<T> {
    private float score;
    private T result;

    public ScoredResult() {
    }

    public static <T> ScoredResult<T> of(float score, T resource) {
        ScoredResult<T> sr = new ScoredResult<>();
        sr.setScore(score);
        sr.setResult(resource);
        return sr;
    }

    public <U> ScoredResult<U> map(Function<? super T, ? extends U> converter) {
        ScoredResult<U> sr = new ScoredResult<>();
        sr.setScore(this.score);
        sr.setResult(converter.apply(this.result));
        return sr;
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
}
