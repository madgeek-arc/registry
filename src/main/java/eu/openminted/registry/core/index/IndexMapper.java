package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.domain.ResourceType;

import java.util.List;

/**
 * Created by antleb on 5/20/16.
 */
public interface IndexMapper {

	public List<IndexField> getIndexFields();

	public List<IndexedField> getValues(String resource, ResourceType resourceType);
}
