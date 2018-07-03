package eu.openminted.registry.core.domain.index;

import javax.persistence.Entity;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class IntegerIndexedField extends IndexedField<Integer> {

    public IntegerIndexedField(){

    }

    public IntegerIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(Object::toString).collect(Collectors.toSet()));
    }

}
