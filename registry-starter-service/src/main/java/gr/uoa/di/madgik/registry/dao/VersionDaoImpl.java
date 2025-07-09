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

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository("versionDao")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        readOnly = true)
public class VersionDaoImpl extends AbstractDao<Version> implements VersionDao {

    @Override
    public Version getVersion(Resource resource, String version) {

        CriteriaQuery<Version> criteriaQuery = getCriteriaQuery();
        Root<Version> root = criteriaQuery.from(Version.class);

        criteriaQuery.distinct(true);


        List<Predicate> predicates = new ArrayList<Predicate>();
        predicates.add(getCriteriaBuilder().equal(root.get("resource"), resource));
        predicates.add(getCriteriaBuilder().equal(root.get("version"), version));

        // FIX for version duplicates (to ensure compatibility with existing dbs):
        // replaced getSingleResult() with getResultList() in case there are duplicates in db (bug)
        // sort by desc(creationDate) to get latest version
        criteriaQuery
                .select(root)
                .where(predicates.toArray(new Predicate[]{}))
                .orderBy(getCriteriaBuilder().desc(root.get("creationDate")));

        List<Version> versions = getEntityManager().createQuery(criteriaQuery).getResultList();
        return versions.getFirst();

    }

    @Override
    public List<Version> getVersionsByResource(Resource resource) {
        return getEntityManager().createNativeQuery("SELECT * from resourceversion WHERE reference_id='" + resource.getId() + "' or parent_id='" + resource.getId() + "'", Version.class).getResultList();
    }

    @Override
    public List<Version> getVersionsByResourceType(ResourceType resourceType) {
        return getList("resourceType", resourceType);
    }


    @Override
    public List<Version> getAllVersions() {
        return getList();
    }

    @Override
    public List<Version> getOrphans() {
        List<Version> versions = getEntityManager().createNativeQuery("SELECT * from resourceversion INNER JOIN (SELECT max(creation_date) as maxd, parent_id as zulu FROM resourceversion WHERE reference_id IS NULL GROUP BY parent_id) as tablzor ON maxd=creation_date AND parent_id=zulu", Version.class).getResultList();
        return versions;
    }

    @Override
    @Transactional
    public void addVersion(Version version) {
        persist(version);
    }

    @Override
    @Transactional
    public void updateVersion(Version version) {
        update(version);
    }

    @Override
    @Transactional
    public void updateParent(Resource resource, ResourceType oldResourceType, ResourceType newResourceType) {
        Query query = getEntityManager().createNativeQuery("UPDATE resourceversion SET parent_id='" + resource.getId() + "', reference_id='" + resource.getId() + "',fk_name_version='" + newResourceType.getName() + "', resourcetype_name='" + newResourceType.getName() + "' WHERE parent_id='" + resource.getId() + "' OR reference_id='" + resource.getId() + "'");
        getEntityManager().joinTransaction();
        query.executeUpdate();
    }

    @Override
    @Transactional
    public void deleteVersion(Version version) {
        delete(version);
    }


}
