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

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository("resourceTypeDao")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        readOnly = true)
public class ResourceTypeDaoImpl extends AbstractDao<ResourceType> implements ResourceTypeDao {

    public ResourceTypeDaoImpl() {
        super();
    }

    public ResourceType getResourceType(String name) {
        return getSingleResult("name", name);
    }

    public List<ResourceType> getAllResourceType() {
        return getList();
    }

    @Override
    public List<ResourceType> getAllResourceTypeByAlias(String alias) {
        Query query = getEntityManager().createQuery("SELECT rt FROM ResourceType rt LEFT JOIN rt.aliases a WHERE rt.aliasGroup = :alias or a = :alias");
        query.setParameter("alias", alias);
        List<ResourceType> results = (List<ResourceType>) query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getAllResourceType(int from, int to) {

        CriteriaQuery<ResourceType> criteriaQuery = getCriteriaQuery();
        Root<ResourceType> root = criteriaQuery.from(ResourceType.class);
        criteriaQuery.distinct(true);
        criteriaQuery.select(root);

        TypedQuery<ResourceType> typedQuery = getEntityManager().createQuery(criteriaQuery);
        if (to == 0) {
            typedQuery.setFirstResult(from);
        } else {
            typedQuery.setFirstResult(from);
            typedQuery.setMaxResults((to - from) + 1);
        }
        return typedQuery.getResultList();
    }

    @Override
    @Transactional
    public void addResourceType(ResourceType resourceType) {
        super.persist(resourceType);
    }

    @Override
    public Set<IndexField> getResourceTypeIndexFields(String name) {
        Set<IndexField> indexFields = new HashSet<>();
        Query query = getEntityManager().createQuery("from IndexField where resourceType in " +
                "(from ResourceType rt LEFT JOIN rt.aliases a WHERE rt.name = :name OR rt.aliasGroup = :name OR a = :name)");
        query.setParameter("name", name);
        indexFields.addAll(query.getResultList());
        return indexFields;
    }

    @Override
    @Transactional
    public void deleteResourceType(String resourceType) {
        super.delete(getResourceType(resourceType));
    }

}
