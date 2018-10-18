package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Repository("versionDao")
public class VersionDaoImpl extends AbstractDao<Version> implements VersionDao {

	@Override
	public Version getVersion(Resource resource, String version) {

		criteriaQuery = getCriteriaQuery();
		root = criteriaQuery.from(Version.class);

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
	public void addVersion(Version version) {
		persist(version);
	}

	@Override
	public void deleteVersion(Version version) {
		delete(version);
	}


}
