package eu.openminted.registry.core.domain.index;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class FloatIndexedField extends IndexedField<Double> {

    @Column
    @ElementCollection
    private Set<Double> values;

    @Override
    public Set<Double> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<Double> value) {
        this.values = value;
    }

    public FloatIndexedField() {

    }

    public FloatIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(x -> ((Double) x)).collect(Collectors.toSet()));
    }
}
