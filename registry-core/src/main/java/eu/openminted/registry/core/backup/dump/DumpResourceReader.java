package eu.openminted.registry.core.backup.dump;

import eu.openminted.registry.core.dao.AbstractDao;
import eu.openminted.registry.core.dao.ResourceDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.index.IndexMapper;
import eu.openminted.registry.core.index.IndexMapperFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Component
@StepScope
@Transactional
public class DumpResourceReader extends AbstractDao<String, Resource> implements ItemReader<Resource>, StepExecutionListener {

    private static final Logger logger = LogManager.getLogger(DumpResourceReader.class);

    private ScrollableResults resources;

    private ResourceTypeDao resourceTypeDao;

    private Session session;

    private IndexMapper indexMapper;

    private ResourceType resourceType;

//    @Value("#{stepExecutionContext['resourceType']}")
//    private String resourceType;

    @Autowired
    public DumpResourceReader(ResourceTypeDao resourceTypeDao) {
        this.resourceTypeDao = resourceTypeDao;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String resourceTypeName = stepExecution.getExecutionContext().getString("resourceType");
        Thread.currentThread().setName(resourceTypeName + "_dump");
        session = getSession().getSessionFactory().openSession();
//        session.setReadOnly(Resource.class,true);
        resourceType = resourceTypeDao.getResourceType(resourceTypeName);
        resources = session
                .createCriteria(Resource.class)
                .add(Restrictions.eq("resourceType", resourceType))
                .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)
                .scroll();
        IndexMapperFactory indexMapperFactory = new IndexMapperFactory();
        try {
            indexMapper = indexMapperFactory.createIndexMapper(resourceType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        session.close();
        return ExitStatus.COMPLETED;
    }

    @Override
    public Resource read() throws Exception {
        if(resources.next()) {
            Resource resource = (Resource) resources.get()[0];
            if(resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()) {
                resource.setIndexedFields(indexMapper.getValues(resource.getPayload(),resourceType));
            }
            logger.info(resource.getId());
            return resource;
        }
        return null;
    }
}
