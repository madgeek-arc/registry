package gr.uoa.di.madgik.registry.domain.index;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by stefanos on 9/1/2017.
 */
@Entity
@Table
public class BooleanIndexedField extends IndexedField<Boolean> {

    @Column/*(name = "vals")*/ // TODO: change column name because "values" is a db reserved keyword.
    @ElementCollection
    private Set<Boolean> values;

    public BooleanIndexedField() {
    }

    public BooleanIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(x -> ((Boolean) x)).collect(Collectors.toSet()));
    }

    @Override
    public Set<Boolean> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<Boolean> value) {
        this.values = value;
    }
}
