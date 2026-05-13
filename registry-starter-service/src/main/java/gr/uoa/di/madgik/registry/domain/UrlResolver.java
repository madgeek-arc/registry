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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class UrlResolver {

    private static final Logger logger = LoggerFactory.getLogger(UrlResolver.class);

    public static String getText(String url) throws Exception {
        try (Scanner scanner = new Scanner(new URL(url).openStream(), StandardCharsets.UTF_8)) {
            String out = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : null;
            return (out == null || out.isEmpty()) ? null : out;
        }
    }
}

		

