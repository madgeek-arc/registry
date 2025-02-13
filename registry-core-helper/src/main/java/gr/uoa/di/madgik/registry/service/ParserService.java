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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.Resource;

import java.util.Arrays;

/**
 * Created by stefanos on 4/7/2017.
 */
public interface ParserService {

    <T> T deserialize(Resource resource, Class<T> returnType);

    String serialize(Object resource, ParserServiceTypes mediaType);

    enum ParserServiceTypes {
        JSON("json"),
        XML("xml");

        private final String type;

        ParserServiceTypes(final String type) {
            this.type = type;
        }

        public static ParserServiceTypes fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(ParserServiceTypes.values())
                    .filter(v -> v.type.equalsIgnoreCase(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

        public String getKey() {
            return type;
        }
    }
}

