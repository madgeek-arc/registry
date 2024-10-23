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
