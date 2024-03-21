package gr.uoa.di.madgik.registry.index;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;

import java.util.List;

/**
 * Created by antleb on 5/20/16.
 */
public interface IndexMapper {

    List<IndexField> getIndexFields();

    List<IndexedField> getValues(String resource, ResourceType resourceType);

}
