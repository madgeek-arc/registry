package eu.openminted.registry.core.domain.index;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table
public class IntegerIndexedField extends IndexedField<Integer> {

    @Column
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private Set<Integer> values;

    public IntegerIndexedField(){

    }

    @Override
    public Set<Integer> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<Integer> value) {
        this.values = value;
    }

    public IntegerIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(Object::toString).mapToInt(Integer::parseInt).boxed().collect(Collectors.toSet()));
    }

}
