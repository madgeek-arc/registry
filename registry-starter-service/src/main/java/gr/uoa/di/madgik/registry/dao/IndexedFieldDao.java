package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;

import java.util.List;

public interface IndexedFieldDao {

    List<IndexedField> getIndexedFieldsOfResource(Resource resource);

    void deleteAllIndexedFields(Resource resource);

}
