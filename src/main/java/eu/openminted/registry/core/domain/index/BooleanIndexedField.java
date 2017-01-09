package eu.openminted.registry.core.domain.index;



import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by stefanos on 9/1/2017.
 */
@Entity
public class BooleanIndexedField extends IndexedField<Boolean> {

    @Column
    @ElementCollection
    private Set<Boolean> values;

    public BooleanIndexedField() {
    }

    public BooleanIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(Boolean.class::cast)
                .map(Boolean::booleanValue).collect(Collectors.toSet()));
        setType(String.class.getName());
    }

    @Override
    public Set<Boolean> getValues() {
        Hibernate.initialize(values);
        return values;
    }

    @Override
    public void setValues(Set<Boolean> value) {
        this.values = value;
    }
}
