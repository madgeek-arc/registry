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
import gr.uoa.di.madgik.registry.domain.Resource;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

@Component
@StepScope
public class DumpResourcePartitioner extends AbstractDao<Resource> implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(DumpResourcePartitioner.class);

    private static final int THRESHOLD = 50;

    private final String resourceType;

    public DumpResourcePartitioner(
            @Value("#{stepExecutionContext['resourceType']}") String resourceType,
            @Value("#{jobParameters['resourceType']}") String jobResourceType
    ) {
        this.resourceType = resourceType == null ? jobResourceType : resourceType;
    }


    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Map<String, ExecutionContext> partition(int gridSize) {
        CriteriaBuilder qb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = qb.createQuery(Long.class);
        Root<Resource> root = criteriaQuery.from(Resource.class);
        criteriaQuery.select(qb.count(root));
        criteriaQuery.where(qb.equal(root.get("resourceType").get("name"), resourceType));
        Query query = getEntityManager().createQuery(criteriaQuery);
        Long size = (Long) query.getSingleResult();
        logger.info("Found " + size + " resources for resource type " + this.resourceType);
        return splitRange(size.intValue(), gridSize);
    }

    private Map<String, ExecutionContext> splitRange(int size, int partitions) {
        Map<String, ExecutionContext> versionMap = new HashMap<>(partitions);
        int diff = (int) Math.ceil((double) size / (double) partitions);
        if (diff < THRESHOLD) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("from", 0);
            context.putInt("to", size);
            context.putString("resourceType", resourceType);
            versionMap.put(String.format("%s[%d-%d]", resourceType, 0, size > 0 ? size - 1 : size), context);
        } else {
            IntStream.range(0, partitions).map(x -> x * diff).forEach(from -> {
                ExecutionContext context = new ExecutionContext();
                int to = from + diff;
                to = Math.min(to, size);
                context.putInt("from", from);
                context.putInt("to", to);
                context.putString("resourceType", resourceType);
                versionMap.put(String.format("%s[%d-%d]", resourceType, from, to - 1), context);
            });
        }
        return versionMap;
    }
}
