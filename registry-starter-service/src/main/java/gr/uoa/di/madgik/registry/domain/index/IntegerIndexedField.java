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

package gr.uoa.di.madgik.registry.domain.index;

import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table
public class IntegerIndexedField extends IndexedField<Long> {

    @Column/*(name = "vals")*/ // TODO: change column name because "values" is a db reserved keyword.
    @ElementCollection
    private Set<Long> values;

    public IntegerIndexedField() {

    }

    public IntegerIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(x -> ((Long) x)).collect(Collectors.toSet()));
    }

    @Override
    public Set<Long> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<Long> value) {
        this.values = value;
    }

}
