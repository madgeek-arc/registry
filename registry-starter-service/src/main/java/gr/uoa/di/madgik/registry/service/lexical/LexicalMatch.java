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

import java.util.function.Function;

/**
 * SQL fragments produced by a {@link LexicalSearchStrategy} for a given keyword, parameterized
 * on the table alias they'll be spliced under (the outer {@code ar} alias for direct search
 * queries, the {@code f} alias inside the hybrid search CTE, etc).
 *
 * @param wherePredicate  a boolean SQL expression selecting matching rows
 * @param rankExpression  a numeric SQL expression usable both for {@code ORDER BY} and for
 *                        deriving a hybrid-search RRF rank via {@code ROW_NUMBER()}
 */
public record LexicalMatch(Function<String, String> wherePredicate, Function<String, String> rankExpression) {

    public String wherePredicate(String tableAlias) {
        return wherePredicate.apply(tableAlias);
    }

    public String rankExpression(String tableAlias) {
        return rankExpression.apply(tableAlias);
    }

    /** No keyword was given at all — browse everything, matching the blank-keyword contract. */
    static LexicalMatch matchEverything() {
        return new LexicalMatch(alias -> "TRUE", alias -> "0");
    }

    /** A keyword was given but nothing significant remained after stop-word filtering. */
    static LexicalMatch matchNothing() {
        return new LexicalMatch(alias -> "FALSE", alias -> "0");
    }
}
