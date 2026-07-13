/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    @Override
    @SuppressWarnings("unchecked")
    public ResourceType getPersistedSnapshot(String name) {
        // FlushModeType.COMMIT on every query here, in the order they run, for the same reason as
        // ResourceDaoImpl.getPersistedContent(): none of them may trigger Hibernate's default
        // auto-flush-before-query, or a pending in-memory change on an already-managed ResourceType
        // (or IndexField) for this name would get flushed to the database before we read it.
        // Scalar/projection selects (not entity fetches) also mean Hibernate has no managed instance
        // to substitute in place of what the database actually returns. Callers must run this
        // before anything else touches ResourceType, ResourceType_aliases, ResourceType_properties,
        // or IndexField for this name in the same call.
        List<Object[]> scalarRows = getEntityManager()
                .createQuery("SELECT rt.schema, rt.schemaUrl, rt.payloadType, rt.indexMapperClass FROM ResourceType rt WHERE rt.name = :name", Object[].class)
                .setFlushMode(FlushModeType.COMMIT)
                .setParameter("name", name)
                .getResultList();
        if (scalarRows.isEmpty()) {
            return null;
        }
        Object[] scalarRow = scalarRows.get(0);

        Set<String> aliases = new HashSet<>(getEntityManager()
                .createQuery("SELECT a FROM ResourceType rt JOIN rt.aliases a WHERE rt.name = :name", String.class)
                .setFlushMode(FlushModeType.COMMIT)
                .setParameter("name", name)
                .getResultList());

        Map<String, String> properties = new HashMap<>();
        for (Object[] row : (List<Object[]>) (List<?>) getEntityManager()
                .createQuery("SELECT KEY(p), VALUE(p) FROM ResourceType rt JOIN rt.properties p WHERE rt.name = :name", Object[].class)
                .setFlushMode(FlushModeType.COMMIT)
                .setParameter("name", name)
                .getResultList()) {
            properties.put((String) row[0], (String) row[1]);
        }

        List<IndexField> indexFields = new ArrayList<>();
        for (Object[] row : (List<Object[]>) (List<?>) getEntityManager()
                .createQuery("SELECT f.name, f.path, f.type, f.label, f.defaultValue, f.multivalued, f.primaryKey, "
                        + "f.searchCapabilities, f.embeddingWeight, f.relatedResourceType, f.relatedResourceTypeField "
                        + "FROM IndexField f WHERE f.resourceType.name = :name", Object[].class)
                .setFlushMode(FlushModeType.COMMIT)
                .setParameter("name", name)
                .getResultList()) {
            IndexField field = new IndexField();
            field.setName((String) row[0]);
            field.setPath((String) row[1]);
            field.setType((String) row[2]);
            field.setLabel((String) row[3]);
            field.setDefaultValue((String) row[4]);
            field.setMultivalued((boolean) row[5]);
            field.setPrimaryKey((boolean) row[6]);
            field.setSearchCapabilities(IndexField.normalizeSearchCapabilities((Set<SearchCapability>) row[7]));
            field.setEmbeddingWeight(IndexField.normalizeEmbeddingWeight((Float) row[8], (String) row[2]));
            field.setRelatedResourceType((String) row[9]);
            field.setRelatedResourceTypeField((String) row[10]);
            indexFields.add(field);
        }

        // "not_set" is the persisted sentinel for "no schema URL" (see
        // ResourceTypeServiceImpl.normalizeResourceType), which canonicalizes it - and null - down
        // to null before ResourceTypeChangeDetector.hasSameDefinition ever runs on a candidate.
        // Reading the raw column here without the same canonicalization would make every
        // schema-based (non-URL) resource type compare as "changed" against its own unchanged self.
        String schemaUrl = (String) scalarRow[1];
        if ("not_set".equals(schemaUrl)) {
            schemaUrl = null;
        }

        ResourceType snapshot = new ResourceType();
        snapshot.setName(name);
        snapshot.setSchema((String) scalarRow[0]);
        snapshot.setSchemaUrl(schemaUrl);
        snapshot.setPayloadType((String) scalarRow[2]);
        snapshot.setIndexMapperClass((String) scalarRow[3]);
        snapshot.setAliases(aliases);
        snapshot.setProperties(properties);
        snapshot.setIndexFields(indexFields);
        return snapshot;
    }

    public List<ResourceType> getAllResourceType() {
        return getList();
    }

    @Override
    public List<ResourceType> getAllResourceTypeByAlias(String alias) {
        Query query = getEntityManager().createQuery("SELECT DISTINCT rt FROM ResourceType rt LEFT JOIN rt.aliases a WHERE a = :alias");
        query.setParameter("alias", alias);
        return (List<ResourceType>) query.getResultList();
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
    @Transactional
    public ResourceType updateResourceType(ResourceType resourceType) {
        return super.update(resourceType);
    }

    @Override
    public Set<IndexField> getResourceTypeIndexFields(String name) {
        Set<IndexField> indexFields = new HashSet<>();
        Query query = getEntityManager().createQuery(
                "select distinct idx " +
                        "from IndexField idx " +
                        "join idx.resourceType rt " +
                        "left join rt.aliases a " +
                        "where rt.name = :name or a = :name"
        );
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
