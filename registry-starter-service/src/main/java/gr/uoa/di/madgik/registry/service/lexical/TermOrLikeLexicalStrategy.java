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
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Term-based OR'd {@code LIKE} lexical strategy: the (stop-word-filtered) query is tokenized
 * into significant terms, each bound as its own {@code LIKE '%term%'} parameter, and the terms
 * are OR'd together — replacing the previous single-phrase substring match with per-term
 * matching, mirroring Elasticsearch's default {@code multi_match} OR semantics.
 */
@Component
public class TermOrLikeLexicalStrategy implements LexicalSearchStrategy {

    public static final String NAME = "term-like";

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

        List<String> paramNames = IntStream.range(0, terms.size())
                .mapToObj(i -> "term" + i)
                .toList();
        for (int i = 0; i < terms.size(); i++) {
            params.addValue(paramNames.get(i), "%" + terms.get(i).toLowerCase(Locale.ROOT) + "%");
        }

        return new LexicalMatch(
                alias -> paramNames.stream()
                        .map(p -> "lower(%s.payload) LIKE :%s".formatted(alias, p))
                        .collect(Collectors.joining(" OR ", "(", ")")),
                alias -> paramNames.stream()
                        .map(p -> "(lower(%s.payload) LIKE :%s)::int".formatted(alias, p))
                        .collect(Collectors.joining(" + "))
        );
    }
}
