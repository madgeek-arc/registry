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

class TermOrLikeLexicalStrategyTest {

    private final TermOrLikeLexicalStrategy strategy = new TermOrLikeLexicalStrategy();

    @Test
    void name_isTermLike() {
        assertEquals("term-like", strategy.name());
    }

    @Test
    void build_orsSignificantTermsAsLikePredicates() {
        MapSqlParameterSource params = new MapSqlParameterSource();

        LexicalMatch match = strategy.build("restaurants in athens near the airport", params);

        assertEquals(
                "(lower(ar.payload) LIKE :term0 OR lower(ar.payload) LIKE :term1 OR lower(ar.payload) LIKE :term2 OR lower(ar.payload) LIKE :term3)",
                match.wherePredicate("ar"));
        assertEquals("%restaurants%", params.getValue("term0"));
        assertEquals("%athens%", params.getValue("term1"));
        assertEquals("%near%", params.getValue("term2"));
        assertEquals("%airport%", params.getValue("term3"));
    }

    @Test
    void build_rankExpressionCountsMatchedTerms() {
        MapSqlParameterSource params = new MapSqlParameterSource();

        LexicalMatch match = strategy.build("athens airport", params);

        assertEquals(
                "(lower(ar.payload) LIKE :term0)::int + (lower(ar.payload) LIKE :term1)::int",
                match.rankExpression("ar"));
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

        assertEquals("(lower(f.payload) LIKE :term0)", match.wherePredicate("f"));
    }
}
