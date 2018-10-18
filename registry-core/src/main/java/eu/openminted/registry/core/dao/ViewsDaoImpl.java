package eu.openminted.registry.core.dao;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Version;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;


@Repository("viewsDao")
public class ViewsDaoImpl extends AbstractDao<Version> implements ViewsDao {

	private static Logger logger = LogManager.getLogger(ViewsDaoImpl.class);

	@Override
	public void createView(ResourceType resourceType) {
        String selectFields = "";
        String joins = "";
        int count = 0;
        if (resourceType.getIndexFields() != null) {
            for (IndexField indexField : resourceType.getIndexFields()) {
                String indexFieldString = "";
                if(indexField.isMultivalued())
                    selectFields = selectFields.concat("coalesce (" + indexField.getName() + ".values, '{}') as " + indexField.getName());
                else
                    selectFields = selectFields.concat("coalesce (" + indexField.getName() + ".values, '') as " + indexField.getName());
                switch (indexField.getType()) {
                    case "java.lang.Float":
                        indexFieldString = "floatindexedfield";
                        break;
                    case "java.lang.Integer":
                        indexFieldString = "integerindexedfield";
                        break;
                    case "java.lang.String":
                        indexFieldString = "stringindexedfield";
                        break;
                    case "java.lang.Boolean":
                        indexFieldString = "booleanindexedfield";
                        break;
                    case "java.lang.Long":
                        indexFieldString = "longindexedfield";
                        break;
                    case "java.util.Date":
                        indexFieldString = "dateindexedfield";
                        break;
                    default:
                        throw new ServiceException("Unrecognised indexed field type");

                }
                if (!indexField.isMultivalued()) {

                    joins = joins.concat(" left join (select r.id, ifv.values" +
                            " from resource r " +
                            " join " + indexFieldString + " if on if.resource_id=r.id " +
                            " join " + indexFieldString + "_values ifv on ifv." + indexFieldString + "_id=if.id " +
                            " where if.name='" + indexField.getName() + "') as " + indexField.getName() + " on  " + indexField.getName() + ".id=r.id ");
                } else {
                    joins = joins.concat(" left join (select r.id, array_agg(ifv.values) as values" +
                            " from resource r " +
                            " join " + indexFieldString + " if on if.resource_id=r.id " +
                            " join " + indexFieldString + "_values ifv on ifv." + indexFieldString + "_id=if.id " +
                            " where if.name='" + indexField.getName() + "' group by r.id) as " + indexField.getName() + " on  " + indexField.getName() + ".id=r.id  ");
                }
                if (count != resourceType.getIndexFields().size() - 1) {
                    selectFields = selectFields.concat(" , ");
                }
                count++;
            }

            Query query = getEntityManager().createNativeQuery("CREATE OR REPLACE VIEW " + resourceType.getName() + "_view AS select r.id, " + selectFields + " from resource r " + joins + " where r.fk_name='" + resourceType.getName() + "'");
            try {
                getEntityManager().getTransaction().begin();
                query.executeUpdate();
                getEntityManager().getTransaction().commit();
            } catch (Exception e) {
                logger.info("View was not created",e);
            }
        }
    }

	@Override
	public void deleteView(String resourceType) {
	    getEntityManager().getTransaction().begin();
		getEntityManager().createNativeQuery("DROP VIEW IF EXISTS "+resourceType+"_view").executeUpdate();
        getEntityManager().getTransaction().commit();
	}


}
