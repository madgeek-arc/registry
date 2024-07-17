package gr.uoa.di.madgik.registry.dao;

import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractDao<T> {

    private final Class<T> persistentClass;

    @PersistenceContext(unitName = "registryEntityManagerFactory")
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public AbstractDao() {
        this.persistentClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Scope("request")
    protected CriteriaBuilder getCriteriaBuilder() {
        return entityManager.getCriteriaBuilder();
    }

    @Scope("request")
    protected CriteriaQuery<T> getCriteriaQuery() {
        return getCriteriaBuilder().createQuery(persistentClass);
    }

    protected EntityManager getEntityManager() {
        return this.entityManager;
    }

    @SuppressWarnings("unchecked")
    public T getSingleResult(String key, Object value) {
        CriteriaQuery<T> criteriaQuery = getCriteriaQuery();
        Root<T> root = criteriaQuery.from(persistentClass);

        criteriaQuery.distinct(true);
        criteriaQuery.select(root).where(getCriteriaBuilder().equal(root.get(key), value));
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);

        return query.getResultList().isEmpty() ? null : query.getSingleResult();
    }

    public List<T> getList(String key, Object value) {
        CriteriaQuery<T> criteriaQuery = getCriteriaQuery();
        Root<T> root = criteriaQuery.from(persistentClass);

        criteriaQuery.distinct(true);
        criteriaQuery.select(root).where(getCriteriaBuilder().equal(root.get(key), value));
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public List<T> getList() {
        CriteriaQuery<T> criteriaQuery = getCriteriaQuery();
        Root<T> root = criteriaQuery.from(persistentClass);

        criteriaQuery.distinct(true);
        criteriaQuery.select(root);
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public Stream<T> getStream() {
        CriteriaQuery<T> criteriaQuery = getCriteriaQuery();
        Root<T> root = criteriaQuery.from(persistentClass);

        criteriaQuery.select(root);
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
        return query.getResultStream();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public T update(T entity) {
        return entityManager.merge(entity);
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public void persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(T entity) {
        entityManager.remove(entity);
        entityManager.flush();
    }

}
