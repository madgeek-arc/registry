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

import gr.uoa.di.madgik.registry.domain.StopWordFilter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Postgres full-text-search lexical strategy: matches against the generated {@code search_vector}
 * column (see the {@code V6.0} migration) using {@code to_tsquery}.
 *
 * <p>Terms are OR'd via the explicit {@code |} operator rather than
 * {@code plainto_tsquery}/{@code websearch_to_tsquery} (which AND space-separated words by
 * default), mirroring Elasticsearch's default {@code multi_match} OR semantics. Each term still
 * passes through the {@code english} dictionary inside {@code to_tsquery}, preserving
 * stemming/lexeme normalization.</p>
 */
@Component
public class FullTextLexicalStrategy implements LexicalSearchStrategy {

    public static final String NAME = "full-text";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public LexicalMatch build(String keyword, MapSqlParameterSource params) {
        List<String> terms = StopWordFilter.filterStopWords(keyword);
        if (terms.isEmpty()) {
            return StringUtils.hasText(keyword) ? LexicalMatch.matchNothing() : LexicalMatch.matchEverything();
        }

        // Terms come from StopWordFilter's \W+ tokenization (alphanumeric only), so no
        // to_tsquery operator characters (&, |, !, <->, parentheses) can appear in them.
        String tsQueryText = terms.stream().collect(Collectors.joining(" | "));
        params.addValue("tsQueryText", tsQueryText);

        return new LexicalMatch(
                alias -> "%s.search_vector @@ to_tsquery('english', :tsQueryText)".formatted(alias),
                alias -> "ts_rank(%s.search_vector, to_tsquery('english', :tsQueryText))".formatted(alias)
        );
    }
}
