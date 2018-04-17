package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.springframework.stereotype.Repository;

@Repository("viewsDao")
public class ViewsDaoImpl extends AbstractDao<String, String> implements ViewsDao {

	private static Logger logger = LogManager.getLogger(ViewsDaoImpl.class);

	@Override
	public void createView(ResourceType resourceType) {
		String selectFields = "";
		String joins = "";
		int count=0;
		for(IndexField indexField : resourceType.getIndexFields()){

			selectFields = selectFields.concat(indexField.getName()+".values as "+ indexField.getName());

			if(!indexField.isMultivalued()) {
				joins = joins.concat(" join (select r.id, ifv.values" +
						" from resource r " +
						" join indexedfield if on if.resource_id=r.id " +
						" join indexedfield_values ifv on ifv.indexedfield_id=if.id " +
						" where if.name='" + indexField.getName() + "') as " + indexField.getName()+" on  " + indexField.getName()+".id=r.id ");
			}else{
				joins = joins.concat(" join (select r.id, array_agg(ifv.values) as values" +
						" from resource r " +
						" join indexedfield if on if.resource_id=r.id " +
						" join indexedfield_values ifv on ifv.indexedfield_id=if.id " +
						" where if.name='" + indexField.getName()+"' group by r.id) as " + indexField.getName()+" on  " + indexField.getName()+".id=r.id  ");
			}
			if(count!=resourceType.getIndexFields().size()-1){
				selectFields = selectFields.concat(" , ");
			}
			count++;
		}

		Query query = getSession().createSQLQuery("CREATE VIEW "+resourceType.getName()+"_view AS select r.id, " + selectFields + " from resource r " + joins + " where r.fk_name='"+resourceType.getName()+"'");

		query.executeUpdate();
	}

	@Override
	public void deleteView(String resourceType) {
		getSession().createSQLQuery("DROP VIEW "+resourceType+"_view").executeUpdate();
	}


}
