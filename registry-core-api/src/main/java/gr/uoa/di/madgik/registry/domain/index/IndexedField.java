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

import com.fasterxml.jackson.annotation.JsonIgnore;
import gr.uoa.di.madgik.registry.domain.Resource;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by antleb on 5/20/16.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class IndexedField<T> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    private Resource resource;

    @Column
    private String name;

    public IndexedField() {
    }

    public IndexedField(String name, Set<T> values) {
        setName(name);
        setValues(values);
    }

    public abstract Set<T> getValues();

    public abstract void setValues(Set<T> value);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexedField<?> that = (IndexedField<?>) o;

        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return getValues() != null ? getValues().equals(this.getValues()) : that.getValues() == null;

    }

    @Override
    public int hashCode() {
        int result = resource != null ? resource.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (getValues() != null ? getValues().hashCode() : 0);
        return result;
    }
}
