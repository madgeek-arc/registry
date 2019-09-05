package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Repository;

/**
 * Created by antleb on 5/21/16.
 */
@Repository("indexMapperFactory")
public class IndexMapperFactory implements ApplicationContextAware {

	@Autowired
	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	public IndexMapper createIndexMapper(ResourceType resourceType) throws Exception {
//		IndexMapper indexMapper = null;
//
//		String indexMapperClass = resourceType.getIndexMapperClass();
//
//		if (indexMapperClass.equals(DefaultIndexMapper.class.getName()))
//			indexMapper = new DefaultIndexMapper(resourceType.getIndexFields());
//		else
//			indexMapper = (IndexMapper) Class.forName(resourceType.getIndexMapperClass()).newInstance();
//
//		return indexMapper;

		IndexMapper indexMapper = (IndexMapper) context.getBean(Class.forName(resourceType.getIndexMapperClass()));

		if (DefaultIndexMapper.class.isAssignableFrom(Class.forName(resourceType.getIndexMapperClass())))
			((DefaultIndexMapper) indexMapper).setIndexFields(resourceType.getIndexFields());

		return indexMapper;
	}


}
