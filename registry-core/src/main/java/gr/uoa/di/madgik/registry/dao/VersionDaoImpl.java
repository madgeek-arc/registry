package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

        criteriaQuery.select(root).where(predicates.toArray(new Predicate[]{}));

        return getEntityManager().createQuery(criteriaQuery).getSingleResult();

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
