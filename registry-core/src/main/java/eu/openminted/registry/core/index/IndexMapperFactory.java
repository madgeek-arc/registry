package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

/**
 * Created by antleb on 5/21/16.
 */
@Repository("indexMapperFactory")
public class IndexMapperFactory{

	@Autowired
	private ApplicationContext context;

	public IndexMapper createIndexMapper(ResourceType resourceType) throws Exception {

		IndexMapper indexMapper = (IndexMapper) context.getBean(Class.forName(resourceType.getIndexMapperClass()));

		if (DefaultIndexMapper.class.isAssignableFrom(Class.forName(resourceType.getIndexMapperClass())))
			((DefaultIndexMapper) indexMapper).setIndexFields(resourceType.getIndexFields());

		return indexMapper;
	}


}
