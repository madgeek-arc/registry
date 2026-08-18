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

package gr.uoa.di.madgik.registry.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopWordFilterTest {

    @Test
    void removesStopWordsButKeepsSignificantTerms() {
        List<String> terms = StopWordFilter.filterStopWords("restaurants in athens near the airport");
        assertEquals(List.of("restaurants", "athens", "near", "airport"), terms);
    }

    @Test
    void preservesShortMeaningfulAcronyms() {
        List<String> terms = StopWordFilter.filterStopWords("AI UK C R Go TV in the news");
        assertEquals(List.of("AI", "UK", "C", "R", "Go", "TV", "news"), terms);
    }

    @Test
    void deduplicatesWhilePreservingFirstOccurrenceOrder() {
        List<String> terms = StopWordFilter.filterStopWords("athens near athens airport near");
        assertEquals(List.of("athens", "near", "airport"), terms);
    }

    @Test
    void filterStopWordsReturnsEmptyForAllStopWordQuery() {
        assertTrue(StopWordFilter.filterStopWords("the of and").isEmpty());
    }

    @Test
    void filterStopWordsReturnsEmptyForNullOrBlank() {
        assertTrue(StopWordFilter.filterStopWords(null).isEmpty());
        assertTrue(StopWordFilter.filterStopWords("   ").isEmpty());
    }

}
