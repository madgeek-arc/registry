/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an inclusive range constraint for a single indexed field.
 * A null bound means no constraint from that side.
 * When {@code includeNull} is true, records where the field is absent/null
 * are treated as passing the filter (e.g. null expiryDate = never expires).
 */
public class RangeFilter {

    private final Object from;
    private final Object to;
    private final boolean includeNull;

    @JsonCreator
    public RangeFilter(@JsonProperty("from") Object from,
                       @JsonProperty("to") Object to,
                       @JsonProperty("includeNull") boolean includeNull) {
        this.from = from;
        this.to = to;
        this.includeNull = includeNull;
    }

    /** Inclusive lower bound ({@code >=}). Null means no lower constraint. */
    public Object getFrom() {
        return from;
    }

    /** Inclusive upper bound ({@code <=}). Null means no upper constraint. */
    public Object getTo() {
        return to;
    }

    /** When true, records with a null field value pass the filter unconditionally. */
    public boolean isIncludeNull() {
        return includeNull;
    }
}
