package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.List;

public interface IndexFieldDao {

    List<IndexField> getIndexFieldsOfResourceType(ResourceType resourceType);

}
