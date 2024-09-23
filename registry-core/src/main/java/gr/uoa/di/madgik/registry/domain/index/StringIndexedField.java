package gr.uoa.di.madgik.registry.domain.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by antleb on 5/24/16.
 */
@Entity
@Table
public class StringIndexedField extends IndexedField<String> {

    private static final Logger logger = LoggerFactory.getLogger(StringIndexedField.class);

    @Column(columnDefinition = "text"/*, name = "vals"*/)
    // TODO: change column name because "values" is a db reserved keyword.
//	@CollectionTable(name="string_values", joinColumns=@JoinColumn(name="stringindexedfield_id"))
    @ElementCollection
    private Set<String> values;

    public StringIndexedField() {
    }

    public StringIndexedField(String name, Set<Object> values) {
        setName(name);
        setValues(values.stream().map(x -> ((String) x)).collect(Collectors.toSet()));
    }

    @Override
    public Set<String> getValues() {
        return values;
    }

    @Override
    public void setValues(Set<String> value) {
        this.values = value;
    }

}
