package eu.openminted.registry.core.backup.dump;

import eu.openminted.registry.core.dao.AbstractDao;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

@Component
@StepScope
@Transactional
public class DumpResourcePartitioner extends AbstractDao<String, Resource> implements Partitioner {

    private static final Logger logger = LogManager.getLogger(DumpResourcePartitioner.class);

    private static final int THRESHOLD = 50;

    private String resourceType;

    @Autowired
    public DumpResourcePartitioner(
            @Value("#{stepExecutionContext['resourceType']}") String resourceType,
            @Value("#{jobParameters['resourceType']}") String jobResourceType
    ) {
        this.resourceType = resourceType == null ? jobResourceType : resourceType;
    }


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Session session = getSession().getSessionFactory().openSession();
        List sizeResult = session.createCriteria(Resource.class)
                .add(Restrictions.eq("resourceType.name", resourceType))
                .setProjection(Projections.count("id")).list();
        int size = Integer.parseInt(sizeResult.get(0).toString());
        session.close();
        return splitRange(size,gridSize);
    }

    private Map<String, ExecutionContext> splitRange(int size, int partitions) {
        Map<String, ExecutionContext> versionMap = new HashMap<>(partitions);
        int diff = (int)Math.ceil((double)size/(double)partitions);
        if(diff < THRESHOLD) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("from",0);
            context.putInt("to",size);
            context.putString("resourceType",resourceType);
            versionMap.put(String.format("%s[%d-%d]",resourceType,0,size-1),context);
        } else {
            IntStream.range(0,partitions).map(x -> x * diff).forEach(from -> {
                ExecutionContext context = new ExecutionContext();
                int to = from + diff - 1;
                context.putInt("from",from);
                context.putInt("to",(to >= size) ? size -1 : to);
                context.putString("resourceType",resourceType);
                versionMap.put(String.format("%s[%d-%d]",resourceType,from,to),context);
            });
        }
        return versionMap;
    }
}
