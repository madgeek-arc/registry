package eu.openminted.registry.core.backup.dump;

import eu.openminted.registry.core.dao.AbstractDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.index.IndexMapper;
import eu.openminted.registry.core.index.IndexMapperFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

    private int from, to;

    @Autowired
    public DumpResourceReader(ResourceTypeDao resourceTypeDao) {
        this.resourceTypeDao = resourceTypeDao;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String resourceTypeName;
        from = 0;
        to = Integer.MAX_VALUE;
        ExecutionContext context = stepExecution.getExecutionContext();
        if(context.get("resourceType") != null)
            resourceTypeName = context.getString("resourceType");
        else
            resourceTypeName = stepExecution.getJobExecution().getJobParameters().getString("resourceType");
        if(context.get("from") != null && context.get("to") != null) {
            from = context.getInt("from");
            to = context.getInt("to");
        }
        session = getSession().getSessionFactory().openSession();
        resourceType = resourceTypeDao.getResourceType(resourceTypeName);
        resources = session
                .createCriteria(Resource.class)
                .add(Restrictions.eq("resourceType", resourceType))
                .setFirstResult(from)
                .setMaxResults(to-from-1)
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
        if(session.isOpen())
            session.close();
        logger.info(String.format(
                "Read resources of %s [%4d -%4d] skipped=%d retries=%d total=%d",
                resourceType.getName(),
                from,
                to,
                stepExecution.getSkipCount(),
                stepExecution.getRollbackCount(),
                stepExecution.getWriteCount()
                )
        );
        return ExitStatus.COMPLETED;
    }

    @Override
    public Resource read() throws Exception {
        if(to<=0)
            return null;
        try {
            if(resources.next()) {
                Resource resource = (Resource) resources.get()[0];
                if(resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()) {
                    resource.setIndexedFields(indexMapper.getValues(resource.getPayload(),resourceType));
                }
                return resource;
            }
        } catch(Exception e) {
            resources.previous();
            logger.error("Reader",e);
            throw new UnexpectedInputException(e.getMessage());
        }

        return null;
    }
}
