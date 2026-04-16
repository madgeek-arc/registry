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

package gr.uoa.di.madgik.registry.domain.index;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class SearchCapabilitySetConverter implements AttributeConverter<Set<SearchCapability>, String> {

    @Override
    public String convertToDatabaseColumn(Set<SearchCapability> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .sorted()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<SearchCapability> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EnumSet.noneOf(SearchCapability.class);
        }
        EnumSet<SearchCapability> capabilities = EnumSet.noneOf(SearchCapability.class);
        Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(SearchCapability::valueOf)
                .forEach(capabilities::add);
        return capabilities;
    }
}
