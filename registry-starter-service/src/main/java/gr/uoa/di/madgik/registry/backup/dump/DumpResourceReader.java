/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.backup.dump;

import gr.uoa.di.madgik.registry.dao.AbstractDao;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.index.IndexMapper;
import gr.uoa.di.madgik.registry.index.IndexMapperFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
public class DumpResourceReader extends AbstractDao<Resource> implements ItemReader<Resource>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(DumpResourceReader.class);

    private List<Resource> resources;

    private final ResourceTypeDao resourceTypeDao;

    private final IndexMapperFactory indexMapperFactory;

    private IndexMapper indexMapper;

    private ResourceType resourceType;

    private int from, to;

    public DumpResourceReader(ResourceTypeDao resourceTypeDao, IndexMapperFactory indexMapperFactory) {
        this.resourceTypeDao = resourceTypeDao;
        this.indexMapperFactory = indexMapperFactory;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public void beforeStep(StepExecution stepExecution) {
        String resourceTypeName;
        from = 0;
        to = Integer.MAX_VALUE;
        ExecutionContext context = stepExecution.getExecutionContext();
        if (context.get("resourceType") != null)
            resourceTypeName = context.getString("resourceType");
        else
            resourceTypeName = stepExecution.getJobExecution().getJobParameters().getString("resourceType");
        if (context.get("from") != null && context.get("to") != null) {
            from = context.getInt("from");
            to = context.getInt("to");
        }
        resourceType = resourceTypeDao.getResourceType(resourceTypeName);

        CriteriaQuery<Resource> criteriaQuery = getCriteriaQuery();
        Root<Resource> root = criteriaQuery.from(Resource.class);

        criteriaQuery.distinct(true);


        List<Predicate> predicates = new ArrayList<>();
        predicates.add(getCriteriaBuilder().equal(root.get("resourceType"), resourceType));

        criteriaQuery
                .select(root)
                .where(predicates.toArray(new Predicate[]{}))
                .orderBy(getCriteriaBuilder().asc(root.get("id")));

        TypedQuery<Resource> query = getEntityManager().createQuery(criteriaQuery);

        query.setFirstResult(from);
        query.setMaxResults(to - from);
        resources = query.getResultList();
        try {
            logger.info("Just read {} resources for {}", resources.size(), resourceType.getName());
            indexMapper = indexMapperFactory.createIndexMapper(resourceType);
        } catch (Exception e) {
            logger.error("ResourceReader failed on beforeStep with message :{}", e.getMessage(), e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("Read resources of {} [{} - {}] skipped={} retries={} total={}",
                resourceType.getName(),
                from,
                to,
                stepExecution.getSkipCount(),
                stepExecution.getRollbackCount(),
                stepExecution.getWriteCount()
        );
        return ExitStatus.COMPLETED;
    }

    @Override
    public Resource read() {
        if (resources.isEmpty()) {
            return null;
        }

        Resource resource = resources.remove(0);
        if (resource != null) {
            if (resource.getIndexedFields() == null || resource.getIndexedFields().isEmpty()) {
                resource.setIndexedFields(indexMapper.getValues(resource.getPayload(), resourceType));
            }
        }
        return resource;
    }
}
