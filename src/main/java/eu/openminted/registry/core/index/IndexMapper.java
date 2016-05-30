package eu.openminted.registry.core.index;

import java.util.List;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;

/**
 * Created by antleb on 5/20/16.
 */
public interface IndexMapper {

	public List<IndexField> getIndexFields();

	public List<IndexedField> getValues(String resource, ResourceType resourceType);
}
