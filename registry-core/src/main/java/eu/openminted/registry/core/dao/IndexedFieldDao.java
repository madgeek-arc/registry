package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.index.IndexedField;

import java.util.List;

public interface IndexedFieldDao {

    List<IndexedField> getIndexedFieldsOfResource(Resource resource);

}
