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

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JSONFieldParser implements FieldParser {

    private static final Logger logger = LoggerFactory.getLogger(JSONFieldParser.class);

    public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

        Set<Object> response = null;
        try {
            if (isMultiValued) {
                List<Object> answers = JsonPath.read(payload, path);
                if (answers != null) {
                    response = answers
                            .stream()
                            .map(answer -> FieldParser.parseField(fieldType, String.valueOf(answer)))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                }
            } else {
                Object answer = JsonPath.read(payload + "", path);
                if (answer != null) {
                    response = FieldParser.parseField(fieldType, answer.toString());
                }
            }
        } catch (PathNotFoundException e) {
            logger.debug("", e);
        }
        return response;
    }
}
