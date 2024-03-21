package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository("resourceDao")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        readOnly = true)
public class ResourceDaoImpl extends AbstractDao<Resource> implements ResourceDao {

    public ResourceDaoImpl() {
        super();
    }

    public Resource getResource(String id) {
        return getSingleResult("id", id);
    }

    private List<Resource> getSince(Date date, String resourceType, String dateType) {
        CriteriaQuery<Resource> criteriaQuery = getCriteriaQuery();
        Root<Resource> root = criteriaQuery.from(Resource.class);
        Expression<Date> dateTypeExp = root.<Date>get(dateType);
        Expression<String> resourceTypeExp = root.<String>get("resourceType").get("name");
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(getCriteriaBuilder().greaterThan(dateTypeExp, date));
        if (!resourceType.isEmpty())
            predicates.add(getCriteriaBuilder().equal(resourceTypeExp, resourceType));

        criteriaQuery.select(root).where(predicates.toArray(new Predicate[0]));
        TypedQuery<Resource> typedQuery = getEntityManager().createQuery(criteriaQuery);
        return typedQuery.getResultList();
    }

    @Override
    public List<Resource> getModifiedSince(Date date, String resourceType) {
        return getSince(date, resourceType, "modificationDate");
    }

    @Override
    public List<Resource> getModifiedSince(Date date) {
        return getSince(date, "", "modificationDate");
    }

    @Override
    public List<Resource> getCreatedSince(Date date) {
        return getSince(date, "", "creationDate");
    }

    @Override
    public List<Resource> getCreatedSince(Date date, String resourceType) {
        return getSince(date, resourceType, "creationDate");
    }

    @SuppressWarnings("unchecked")
    public List<Resource> getResource(ResourceType resourceType) {
        return getList("resourceType", resourceType);
    }

    public Stream<Resource> getResourceStream() {
        return getStream();
    }

    @SuppressWarnings("unchecked")
    public List<Resource> getResource(ResourceType resourceType, int from, int to) {
        CriteriaQuery<Resource> criteriaQuery = getCriteriaQuery();
        Root<Resource> root = criteriaQuery.from(Resource.class);

        criteriaQuery.select(root);
        Optional<ResourceType> optional = Optional.ofNullable(resourceType);
        optional.ifPresent(r -> criteriaQuery.where(getCriteriaBuilder().equal(root.get("resourceType"), resourceType)));
        criteriaQuery.distinct(true);

        TypedQuery<Resource> typedQuery = getEntityManager().createQuery(criteriaQuery);
        if (to == 0) {
            typedQuery.setFirstResult(from);
        } else {
            int quantity;
            quantity = (from == 0) ? to : to - from;
            typedQuery.setFirstResult(from);
            typedQuery.setMaxResults(quantity);
        }

        return typedQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Resource> getResource(int from, int to) {
        return getResource(null, from, to);
    }

    public List<Resource> getResource() {
        return getResource(null, 0, Integer.MAX_VALUE);
    }

    @Transactional
    public void addResource(Resource resource) {
        persist(resource);
    }

    @Transactional
    public void updateResource(Resource resource) {
        resource.setModificationDate(new Date());
        getEntityManager().merge(resource);
    }

    @Transactional
    public void deleteResource(Resource resource) {
        delete(resource);
    }

}
