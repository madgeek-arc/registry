package eu.openminted.registry.core.domain.index;

import javax.persistence.Entity;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by stefanos on 9/1/2017.
 */
@Entity
public class BooleanIndexedField extends IndexedField<Boolean> {

    public BooleanIndexedField() {
    }

    public BooleanIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(Object::toString).collect(Collectors.toSet()));
    }
}
