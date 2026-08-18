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

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FullTextLexicalStrategyTest {

    private final FullTextLexicalStrategy strategy = new FullTextLexicalStrategy();

    @Test
    void name_isFullText() {
        assertEquals("full-text", strategy.name());
    }

    @Test
    void build_orsSignificantTermsViaToTsquery() {
        MapSqlParameterSource params = new MapSqlParameterSource();

        LexicalMatch match = strategy.build("restaurants in athens near the airport", params);

        assertEquals("ar.search_vector @@ to_tsquery('english', :tsQueryText)", match.wherePredicate("ar"));
        assertEquals("ts_rank(ar.search_vector, to_tsquery('english', :tsQueryText))", match.rankExpression("ar"));
        assertEquals("restaurants | athens | near | airport", params.getValue("tsQueryText"));
    }

    @Test
    void build_blankKeyword_matchesEverything() {
        MapSqlParameterSource params = new MapSqlParameterSource();

        LexicalMatch match = strategy.build("", params);

        assertEquals("TRUE", match.wherePredicate("ar"));
        assertEquals("0", match.rankExpression("ar"));
        assertEquals(0, params.getParameterNames().length);
    }

    @Test
    void build_allStopWordsKeyword_matchesNothing() {
        MapSqlParameterSource params = new MapSqlParameterSource();

        LexicalMatch match = strategy.build("the of", params);

        assertEquals("FALSE", match.wherePredicate("ar"));
        assertEquals("0", match.rankExpression("ar"));
        assertEquals(0, params.getParameterNames().length);
    }

    @Test
    void build_usesRequestedTableAlias() {
        MapSqlParameterSource params = new MapSqlParameterSource();

        LexicalMatch match = strategy.build("athens", params);

        assertEquals("f.search_vector @@ to_tsquery('english', :tsQueryText)", match.wherePredicate("f"));
    }
}
