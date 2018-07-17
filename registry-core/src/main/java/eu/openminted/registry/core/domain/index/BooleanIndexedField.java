package eu.openminted.registry.core.domain.index;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by stefanos on 9/1/2017.
 */
@Entity
@Table
public class BooleanIndexedField extends IndexedField<Boolean> {

    @Column
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private Set<Boolean> values;

    public BooleanIndexedField() {
    }

    @Override
    public Set<Boolean> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<Boolean> value) {
        this.values = value;
    }

    public BooleanIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(Object::toString).map(Boolean::parseBoolean).collect(Collectors.toSet()));
    }
}
