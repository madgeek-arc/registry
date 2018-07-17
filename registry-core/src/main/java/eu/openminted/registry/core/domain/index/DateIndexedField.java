package eu.openminted.registry.core.domain.index;

import eu.openminted.registry.core.service.ServiceException;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table
public class DateIndexedField extends IndexedField<Date> {

    @Column
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private Set<Date> values;


    public DateIndexedField(){

    }

    @Override
    public Set<Date> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<Date> value) {
        this.values = value;
    }

    public DateIndexedField(String name, Set<Object> values) {
        setName(name);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        setValues(values.stream().map(Object::toString).map(s -> {
            try {
                System.out.println(s);
                return sdf.parse(s);
            } catch (ParseException e) {
                throw new ServiceException("Wrong date format for indexed field. Try dd-MM-yyyy");
            }
        }).collect(Collectors.toSet()));
    }
}
