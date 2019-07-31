package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Repository("versionDao")
public class VersionDaoImpl extends AbstractDao<Version> implements VersionDao {

	@Override
	public Version getVersion(Resource resource, String version) {

		CriteriaQuery<Version> criteriaQuery = getCriteriaQuery();
		Root<Version>  root = criteriaQuery.from(Version.class);

		criteriaQuery.distinct(true);


		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(getCriteriaBuilder().equal(root.get("resource"), resource));
		predicates.add(getCriteriaBuilder().equal(root.get("version"), version));

		criteriaQuery.select(root).where(predicates.toArray(new Predicate[]{}));

		return getEntityManager().createQuery(criteriaQuery).getSingleResult();

	}

	@Override
	public List<Version> getVersionsByResource(Resource resource) {
		return getList("resource",resource);
	}

	@Override
	public List<Version> getVersionsByResourceType(ResourceType resourceType) {
		return getList("resourceType",resourceType);
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
	public void addVersion(Version version) {
		persist(version);
	}

	@Override
	public void updateVersion(Version version) {
		update(version);
	}

	@Override
	public void deleteVersion(Version version) {
		delete(version);
	}


}
