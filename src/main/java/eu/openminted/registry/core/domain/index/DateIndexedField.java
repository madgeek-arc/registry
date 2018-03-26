package eu.openminted.registry.core.domain.index;

import javax.persistence.Entity;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class DateIndexedField extends IndexedField<Date> {

    public DateIndexedField(){

    }

    public DateIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(Object::toString).collect(Collectors.toSet()));
    }
}
