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

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * A pluggable SQL lexical-matching approach for the PostgreSQL-backed search implementation.
 * Implementations register as Spring beans and are selected via
 * {@code registry.sql.search.lexical-strategy}.
 */
public interface LexicalSearchStrategy {

    /**
     * The identifier matched against {@code registry.sql.search.lexical-strategy}.
     */
    String name();

    /**
     * Builds the SQL fragments matching {@code keyword}, binding any needed parameters onto
     * {@code params}.
     *
     * @param keyword the raw user keyword (stop-word filtering happens inside the strategy)
     * @param params  the parameter source to bind strategy-specific parameters onto
     * @return the resulting where-predicate/rank-expression pair
     */
    LexicalMatch build(String keyword, MapSqlParameterSource params);
}
