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

package gr.uoa.di.madgik.registry.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractDao<T> {

    private final Class<T> persistentClass;

    @PersistenceContext(unitName = "registryEntityManager")
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

    public Long getTotal(String key, Object value) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = cb.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(persistentClass);

        Optional<Object> val = Optional.ofNullable(value);
        criteriaQuery.select(cb.countDistinct(root));
        val.ifPresent(v -> criteriaQuery.where(cb.equal(root.get(key), v)));

        TypedQuery<Long> query = entityManager.createQuery(criteriaQuery);
        return query.getSingleResult();
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
    public T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(T entity) {
        entityManager.remove(entity);
        entityManager.flush();
    }

}
