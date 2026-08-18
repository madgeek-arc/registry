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

package gr.uoa.di.madgik.registry.service.lexical;

import gr.uoa.di.madgik.registry.configuration.SqlSearchProperties;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the {@link LexicalSearchStrategy} configured via
 * {@code registry.sql.search.lexical-strategy} once at startup, failing fast when the configured
 * name doesn't match any registered strategy bean.
 */
@Service
public class LexicalSearchStrategySelector {

    private final LexicalSearchStrategy active;

    public LexicalSearchStrategySelector(List<LexicalSearchStrategy> strategies, SqlSearchProperties properties) {
        Map<String, LexicalSearchStrategy> byName = strategies.stream()
                .collect(Collectors.toMap(LexicalSearchStrategy::name, Function.identity()));
        String configured = properties.getLexicalStrategy();
        LexicalSearchStrategy resolved = byName.get(configured);
        if (resolved == null) {
            throw new ServiceException(
                    "Unknown registry.sql.search.lexical-strategy '%s'. Known strategies: %s"
                            .formatted(configured, byName.keySet()));
        }
        this.active = resolved;
    }

    public LexicalSearchStrategy active() {
        return active;
    }
}
