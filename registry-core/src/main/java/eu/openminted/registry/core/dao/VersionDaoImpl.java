package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("versionDao")
public class VersionDaoImpl extends AbstractDao<String, Version> implements VersionDao {

	@Override
	public Version getVersion(Resource resource, String version) {
		Criteria cr = getSession().createCriteria(Version.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.eq("resource", resource));
		cr.add(Restrictions.eq("version", version));

		if (cr.list().size() == 0)
			return null;
		else
			return (Version) cr.list().get(0);
	}

	@Override
	public List<Version> getVersionsByResource(Resource resource) {
		Criteria cr = getSession().createCriteria(Version.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.eq("resource", resource));

		return cr.list();
	}

	@Override
	public List<Version> getVersionsByResourceType(ResourceType resourceType) {
		Criteria cr = getSession().createCriteria(Version.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cr.add(Restrictions.eq("resourceType", resourceType));

		return cr.list();
	}


	@Override
	public List<Version> getAllVersions() {
		Criteria cr = getSession().createCriteria(Version.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return cr.list();
	}

	@Override
	public void addVersion(Version version) {
		persist(version);
		getSession().flush();
	}

	@Override
	public void deleteVersion(Version version) {
		getSession().delete(version);
		getSession().flush();
	}


}
