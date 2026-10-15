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

package gr.uoa.di.madgik.registry.dao;

public final class SqlIdentifiers {

    private SqlIdentifiers() {
    }

    /**
     * Quotes a column name so reserved words (e.g. {@code order}) are valid identifiers. The name is
     * lowercased because unquoted identifiers are folded to lowercase by the database.
     */
    public static String quoteColumn(String name) {
        return "\"" + name.toLowerCase().replace("\"", "\"\"") + "\"";
    }
}
