package eu.openminted.registry.core.domain.index;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table
public class DateIndexedField extends IndexedField<Date> {

    @Column/*(name = "vals")*/ // TODO: change column name because "values" is a db reserved keyword.
    @ElementCollection
    private Set<Date> values;


    public DateIndexedField() {

    }

    public DateIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(x -> ((Date) x)).collect(Collectors.toSet()));
    }

    @Override
    public Set<Date> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<Date> value) {
        this.values = value;
    }
}
