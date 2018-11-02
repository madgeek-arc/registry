package eu.openminted.registry.core.backup.dump;

import eu.openminted.registry.core.dao.AbstractDao;
import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.index.IndexMapper;
import eu.openminted.registry.core.index.IndexMapperFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

@Component
@StepScope
@Transactional
public class DumpResourceReader extends AbstractDao<Resource> implements ItemReader<Resource>, StepExecutionListener {

    private static final Logger logger = LogManager.getLogger(DumpResourceReader.class);

    private Iterator<Resource> resources;

    private Stream<Resource> test;

    private ResourceTypeDao resourceTypeDao;

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
        resourceType = resourceTypeDao.getResourceType(resourceTypeName);

        CriteriaQuery<Resource> criteriaQuery = getCriteriaQuery();
        Root<Resource> root = criteriaQuery.from(Resource.class);

        criteriaQuery.distinct(true);


        List<Predicate> predicates = new ArrayList<>();
        predicates.add(getCriteriaBuilder().equal(root.get("resourceType"), resourceType));

        criteriaQuery.select(root).where(predicates.toArray(new Predicate[]{}));

        TypedQuery<Resource> query = getEntityManager().createQuery(criteriaQuery);

        query.setFirstResult(from);
        query.setMaxResults(to-from);
        test = query.getResultStream();
        resources = query.getResultStream().iterator();
        IndexMapperFactory indexMapperFactory = new IndexMapperFactory();
        try {
            indexMapper = indexMapperFactory.createIndexMapper(resourceType);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
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
        if(getEntityManager().getTransaction().isActive())
            getEntityManager().getTransaction().begin();
        if(resources.hasNext()) {
            Resource resource = resources.next();
//            Resource resource = (Resource) resource
            if(resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()) {
                resource.setIndexedFields(indexMapper.getValues(resource.getPayload(),resourceType));
            }
            getEntityManager().getTransaction().commit();
            return resource;
        }
        getEntityManager().getTransaction().commit();
        return null;

    }
}
