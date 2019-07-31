package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;

import java.util.List;

public interface IndexFieldDao {

    List<IndexField> getIndexFieldsOfResourceType(ResourceType resourceType);

}
