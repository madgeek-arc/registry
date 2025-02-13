/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.index;

import gr.uoa.di.madgik.registry.domain.index.*;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;

/**
 * Created by antleb on 5/24/16.
 */
@Component("indexedFieldFactory")
public class IndexedFieldFactory {


    public <T> IndexedField<T> getIndexedField(String fieldName, Set<Object> value, String fieldType) {

        IndexedField field = null;
        if (String.class.getName().equals(fieldType)) {
            field = new StringIndexedField(fieldName, value);
        } else if (Boolean.class.getName().equals(fieldType)) {
            field = new BooleanIndexedField(fieldName, value);
        } else if (Integer.class.getName().equals(fieldType)) {
            field = new IntegerIndexedField(fieldName, value);
        } else if (Date.class.getName().equals(fieldType)) {
            field = new DateIndexedField(fieldName, value);
        } else if (Float.class.getName().equals(fieldType)) {
            field = new FloatIndexedField(fieldName, value);
        } else if (Long.class.getName().equals(fieldType)) {
            field = new LongIndexedField(fieldName, value);
        }
        return field;
    }
}
