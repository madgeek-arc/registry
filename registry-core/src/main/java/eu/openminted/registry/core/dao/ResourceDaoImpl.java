package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository("resourceDao")
@Transactional
public class ResourceDaoImpl extends AbstractDao<Resource> implements ResourceDao {

	public Resource getResource(String id) {
		return getSingleResult("id",id);
	}

	@Override
	public List<Resource> getModifiedSince(Date date){
		CriteriaQuery<Resource> criteriaQuery = getCriteriaQuery();
		Root<Resource> root = criteriaQuery.from(Resource.class);
		criteriaQuery.select(root).where(getCriteriaBuilder().lessThan(root.get("modificationDate"),date));
		TypedQuery<Resource> typedQuery = getEntityManager().createQuery(criteriaQuery);
		return typedQuery.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(ResourceType resourceType) {
		return getList("resourceType", resourceType);
	}

	public Stream<Resource> getResourceStream(){
		return getStream();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(ResourceType resourceType, int from, int to) {
		CriteriaQuery<Resource> criteriaQuery = getCriteriaQuery();
		Root<Resource> root = criteriaQuery.from(Resource.class);

        criteriaQuery.select(root);
        Optional<ResourceType> optional = Optional.ofNullable(resourceType);
        optional.ifPresent(r -> criteriaQuery.where(getCriteriaBuilder().equal(root.get("resourceType"),resourceType)));
		criteriaQuery.distinct(true);

		TypedQuery<Resource> typedQuery = getEntityManager().createQuery(criteriaQuery);
		if (to == 0) {
			typedQuery.setFirstResult(from);
		} else {
			int quantity;
			quantity = (from==0) ? to : to - from;
			typedQuery.setFirstResult(from);
			typedQuery.setMaxResults(quantity);
		}

		return typedQuery.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResource(int from, int to) {
        return getResource(null,from,to);
	}

	public List<Resource> getResource() {
	    return getResource(null,0,Integer.MAX_VALUE);
	}

    @Transactional
	public void addResource(Resource resource){
		persist(resource);
	}

	public void updateResource(Resource resource) {
		resource.setModificationDate(new Date());
		getEntityManager().merge(resource);
	}

	public void deleteResource(Resource resource) {
		delete(resource);
		resource.getResourceType().getResources().remove(resource);
	}

}
