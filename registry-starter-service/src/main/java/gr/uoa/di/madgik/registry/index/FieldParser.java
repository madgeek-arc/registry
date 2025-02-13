/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

import java.time.Instant;
import java.util.Date;
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
                case "java.util.Date":
                    values.add(Date.from(Instant.ofEpochMilli(Long.parseLong(typeValue))));
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
