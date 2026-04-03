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

package gr.uoa.di.madgik.registry.index;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by antleb on 5/21/16.
 */
public interface FieldParser {
    static Set<Object> parseField(String fieldType, String typeValue) {
        Set<Object> values = new HashSet<>();
        if (!StringUtils.isEmpty(typeValue)) {
            switch (fieldType) {
                case "java.lang.String":
                    values.add(typeValue);
                    break;
                case "java.lang.Integer":
                case "java.lang.Long":
                    values.add(Long.parseLong(typeValue));
                    break;
                case "java.lang.Float":
                case "java.lang.Double":
                    values.add(Double.parseDouble(typeValue));
                    break;
                case "java.time.Instant":
                    try {
                        values.add(Instant.ofEpochMilli(Long.parseLong(typeValue)));
                    } catch (NumberFormatException e) {
                        try {
                            // Jackson default: decimal epoch seconds (e.g. "1775228349.752653630")
                            BigDecimal bd = new BigDecimal(typeValue);
                            values.add(Instant.ofEpochSecond(bd.longValue(),
                                    bd.remainder(BigDecimal.ONE).abs().movePointRight(9).longValue()));
                        } catch (NumberFormatException e2) {
                            // ISO-8601 format (e.g. "2026-01-27T16:34:56.438Z")
                            values.add(Instant.parse(typeValue));
                        }
                    }
                    break;
                case "java.lang.Boolean":
                    values.add(Boolean.parseBoolean(typeValue));
                    break;
            }
        }
        return values;
    }

    Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued);
}
