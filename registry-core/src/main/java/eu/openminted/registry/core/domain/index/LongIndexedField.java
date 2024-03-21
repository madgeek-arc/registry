package eu.openminted.registry.core.domain.index;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table
public class LongIndexedField extends IndexedField<Long> {

    @Column/*(name = "vals")*/ // TODO: change column name because "values" is a db reserved keyword.
    @ElementCollection
    private Set<Long> values;

    public LongIndexedField() {

    }

    public LongIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(Object::toString).mapToLong(Long::parseLong).boxed().collect(Collectors.toSet()));
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
