package eu.openminted.registry.core.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.Stream;


@Repository
@Transactional
public abstract class AbstractDao<T> {
     
    private final Class<T> persistentClass;

    protected CriteriaQuery<T> criteriaQuery;

    protected Root<T> root;

    @Autowired
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public AbstractDao(){
        this.persistentClass =(Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected CriteriaBuilder getCriteriaBuilder(){
        return entityManager.getCriteriaBuilder();
    }

    protected CriteriaQuery<T> getCriteriaQuery(){
        return getCriteriaBuilder().createQuery(persistentClass);
    }

    protected EntityManager getEntityManager(){
        return entityManager;
    }
    @SuppressWarnings("unchecked")
    public T getSingleResult(String key, Object value) {
        criteriaQuery = getCriteriaQuery();
        root = criteriaQuery.from(persistentClass);

        criteriaQuery.distinct(true);
        criteriaQuery.select(root).where(getCriteriaBuilder().equal(root.get(key),value));
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);

        return query.getResultList().isEmpty() ? null : query.getSingleResult();
    }

    public List<T> getList(String key, Object value) {
        criteriaQuery = getCriteriaQuery();
        root = criteriaQuery.from(persistentClass);

        criteriaQuery.distinct(true);
        criteriaQuery.select(root).where(getCriteriaBuilder().equal(root.get(key),value));
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public List<T> getList() {
        criteriaQuery = getCriteriaQuery();
        root = criteriaQuery.from(persistentClass);

        criteriaQuery.distinct(true);
        criteriaQuery.select(root);
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

    public Stream<T> getStream() {
        criteriaQuery = getCriteriaQuery();
        root = criteriaQuery.from(persistentClass);

        criteriaQuery.select(root);
        TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
        return query.getResultStream();
    }

    public void persist(T entity) {
        if(!entityManager.getTransaction().isActive())
            entityManager.getTransaction().begin();

        entityManager.persist(entity);
        entityManager.getTransaction().commit();
    }

    public void delete(T entity) {
        if(!entityManager.getTransaction().isActive())
            entityManager.getTransaction().begin();

        entityManager.remove(entity);
        entityManager.getTransaction().commit();
    }
     
}