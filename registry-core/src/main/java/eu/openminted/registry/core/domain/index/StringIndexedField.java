package eu.openminted.registry.core.domain.index;

import eu.openminted.registry.core.dao.SchemaDaoImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by antleb on 5/24/16.
 */
@Entity
@Table
public class StringIndexedField extends IndexedField<String> {

	private static Logger logger = LogManager.getLogger(StringIndexedField.class);

	@Column
	@ElementCollection
	@LazyCollection(LazyCollectionOption.FALSE)
	@Cascade({CascadeType.DELETE, CascadeType.REMOVE})
	private Set<String> values;

	public StringIndexedField() {
	}

	@Override
	public Set<String> getValues() {
		return values;
	}

	@Override
	public void setValues(Set<String> value) {
		this.values = value;
	}

	public StringIndexedField(String name, Set<Object> values) {
		setName(name);
		for(Object value : values)
			logger.info(value + "");
		setValues(values.stream().map(x -> ((String)x)).collect(Collectors.toSet()));
		for(Object value : values)
			logger.info(value + "-----");
	}

}
