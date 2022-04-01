package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;

import java.util.List;

/**
 * Created by antleb on 5/20/16.
 */
public interface IndexMapper {

    List<IndexField> getIndexFields();

    List<IndexedField> getValues(String resource, ResourceType resourceType);

}
