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

package gr.uoa.di.madgik.registry.configuration;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Deserializes {@link Instant} from three formats that appear in legacy payloads:
 * <ul>
 *   <li>ISO-8601 string — {@code "2025-04-28T12:30:00.000Z"} (current canonical form)</li>
 *   <li>Numeric string — {@code "1745842625015"} (epoch millis stored as a JSON string)</li>
 *   <li>JSON number — integer or float interpreted as epoch seconds or millis based on magnitude</li>
 * </ul>
 * Values with more than 10 digits are treated as epoch milliseconds; smaller values as epoch seconds.
 */
class FlexibleInstantDeserializer extends StdDeserializer<Instant> {

    // Threshold: 10-digit max (9_999_999_999 s ≈ year 2286). Anything larger is millis.
    private static final long EPOCH_MILLIS_THRESHOLD = 10_000_000_000L;

    FlexibleInstantDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonToken token = p.currentToken();

        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return fromLong(p.getLongValue());
        }
        if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            double v = p.getDoubleValue();
            return v >= EPOCH_MILLIS_THRESHOLD ? Instant.ofEpochMilli((long) v)
                                               : Instant.ofEpochSecond((long) v);
        }

        String text = p.getText().trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return fromLong(Long.parseLong(text));
        } catch (NumberFormatException ignored) {
        }
        throw InvalidFormatException.from(p, "Cannot parse Instant from value: " + text, text, Instant.class);
    }

    private static Instant fromLong(long v) {
        return v >= EPOCH_MILLIS_THRESHOLD ? Instant.ofEpochMilli(v) : Instant.ofEpochSecond(v);
    }
}
