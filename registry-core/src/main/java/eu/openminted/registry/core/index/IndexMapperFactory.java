package eu.openminted.registry.core.index;

import org.springframework.stereotype.Repository;

import eu.openminted.registry.core.domain.ResourceType;

/**
 * Created by antleb on 5/21/16.
 */
@Repository("indexMapperFactory")
public class IndexMapperFactory {

	public IndexMapper createIndexMapper(ResourceType resourceType) throws Exception {
		IndexMapper indexMapper = null;

		String indexMapperClass = resourceType.getIndexMapperClass();

		if (indexMapperClass.equals(DefaultIndexMapper.class.getName()))
			indexMapper = new DefaultIndexMapper(resourceType.getIndexFields());
		else
			indexMapper = (IndexMapper) Class.forName(resourceType.getIndexMapperClass()).newInstance();

		return indexMapper;
	}
}
