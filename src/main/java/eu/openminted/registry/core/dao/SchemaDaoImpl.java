package eu.openminted.registry.core.dao;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import eu.openminted.registry.core.domain.Schema;

@Repository("schemaDao")
public class SchemaDaoImpl extends AbstractDao<String, Schema> implements SchemaDao{

	@Override
	public Schema getSchema(String id) {
		// TODO Auto-generated method stub
		Criteria cr = getSession().createCriteria(Schema.class);
		cr.add(Restrictions.eq("id", id));
		if(cr.list().size()==0){
			return null;
		}else{
			return (Schema) cr.list().get(0);
		}

	}
	
	@Override
	public Schema getSchemaByUrl(String originalURL) {
		// TODO Auto-generated method stub
		Criteria cr = getSession().createCriteria(Schema.class);
		cr.add(Restrictions.eq("originalUrl", originalURL));
		if(cr.list().size()==0){
			return null;
		}else{
			return (Schema) cr.list().get(0);
		}
	}
	

	@Override
	public void addSchema(Schema schema) {
		getSession().saveOrUpdate(schema);
	}

	@Override
	public void deleteSchema(Schema schema) {
		getSession().delete(schema);
	}

}
