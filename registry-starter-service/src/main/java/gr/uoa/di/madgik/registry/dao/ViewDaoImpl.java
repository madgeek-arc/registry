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

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.service.ServiceException;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Repository("viewsDao")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class ViewDaoImpl extends AbstractDao<Version> implements ViewDao {

    private static final Logger logger = LoggerFactory.getLogger(ViewDaoImpl.class);

    @Override
    public void createView(ResourceType resourceType) {
        if (resourceType.getIndexFields() != null) {
            // create datatype maps
            Map<String, String> dataTypeMap = new HashMap<String, String>();
            dataTypeMap.put("floatindexedfield", "float");
            dataTypeMap.put("integerindexedfield", "bigint");
            dataTypeMap.put("stringindexedfield", "text");
            dataTypeMap.put("booleanindexedfield", "bool");
            dataTypeMap.put("longindexedfield", "bigint");
            dataTypeMap.put("dateindexedfield", "timestamp");

            // order indexFields by name
            resourceType.getIndexFields().sort(Comparator.comparing(IndexField::getName));

            // create maps to store single- and multi-valued index names
            Map<String, List<String>> singleVal_indexMap = new HashMap<String, List<String>>();
            Map<String, List<String>> multiVal_indexMap = new HashMap<String, List<String>>();

            for (IndexField indexField : resourceType.getIndexFields()) {
                String indexFieldString = "";

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
                    // create entry if it does not exist
                    if (!singleVal_indexMap.containsKey(indexFieldString)) {
                        List<String> tempList = new ArrayList<String>();
                        tempList.add(indexField.getName());
                        singleVal_indexMap.put(indexFieldString, tempList);
                    } else {
                        singleVal_indexMap.get(indexFieldString).add(indexField.getName());
                    }
                } else {
                    // create entry if it does not exist
                    if (!multiVal_indexMap.containsKey(indexFieldString)) {
                        List<String> tempList = new ArrayList<String>();
                        tempList.add(indexField.getName());
                        multiVal_indexMap.put(indexFieldString, tempList);
                    } else {
                        multiVal_indexMap.get(indexFieldString).add(indexField.getName());
                    }
                }
            }

            String queryString = "";
            queryString = queryString.concat("CREATE OR REPLACE VIEW " + resourceType.getName() + "_view AS (");
            queryString = queryString.concat("SELECT * FROM (select id, creation_date, modification_date from resource where fk_name='" + resourceType.getName() + "') r");

            // setup query for single-valued indices
            if (!singleVal_indexMap.isEmpty()) {

                // for each indexFieldString in map
                for (String s : singleVal_indexMap.keySet()) {
                    queryString = queryString.concat(" INNER JOIN ");
                    queryString = queryString.concat("(SELECT * FROM crosstab('");
                    queryString = queryString.concat("SELECT i.resource_id, i.name, v.values FROM (");
                    queryString = queryString.concat("SELECT " + s + ".id, " + s + ".name, " + s + ".resource_id FROM resource r, " + s);
                    queryString = queryString.concat(" WHERE r.fk_name = ''" + resourceType.getName() + "'' AND r.id = " + s + ".resource_id AND " + s + ".name IN(");

                    // get list of index names of current subtype
                    Iterator iter = singleVal_indexMap.get(s).iterator();
                    while (iter.hasNext()) {
                        queryString = queryString.concat("''" + iter.next() + "''");
                        if (iter.hasNext()) {
                            queryString = queryString.concat(", ");
                        }
                    }

                    queryString = queryString.concat(") ORDER BY " + s + ".name) i");
                    queryString = queryString.concat(" LEFT JOIN (");
                    queryString = queryString.concat("SELECT " + s + ".id, " + s + "_values.values FROM " + s + ", " + s + "_values");
                    queryString = queryString.concat(" WHERE " + s + ".id = " + s + "_values." + s + "_id) v");
                    queryString = queryString.concat(" ON i.id = v.id");
                    queryString = queryString.concat(" ORDER BY i.resource_id, i.name");
                    queryString = queryString.concat("') AS output_tbl(");
                    queryString = queryString.concat("id varchar(255)");

                    // define output table schema
                    iter = singleVal_indexMap.get(s).iterator();
                    while (iter.hasNext()) {
                        queryString = queryString.concat(", " + iter.next() + " " + dataTypeMap.get(s));
                    }
                    queryString = queryString.concat(") ) s_" + s + " USING(id)");
                }
            }

            // setup query for multi-valued indices
            if (!multiVal_indexMap.isEmpty()) {

                // for each indexFieldString in map
                for (String m : multiVal_indexMap.keySet()) {
                    queryString = queryString.concat(" INNER JOIN ");
                    queryString = queryString.concat("(SELECT * FROM crosstab('");
                    queryString = queryString.concat("SELECT i.resource_id, i.name, array_remove(array_agg(v.values), NULL) FROM (");
                    queryString = queryString.concat("SELECT " + m + ".id, " + m + ".name, " + m + ".resource_id FROM resource r, " + m);
                    queryString = queryString.concat(" WHERE r.fk_name = ''" + resourceType.getName() + "'' AND r.id = " + m + ".resource_id AND " + m + ".name IN(");

                    // get list of index names of current subtype
                    Iterator iter = multiVal_indexMap.get(m).iterator();
                    while (iter.hasNext()) {
                        queryString = queryString.concat("''" + iter.next() + "''");
                        if (iter.hasNext()) {
                            queryString = queryString.concat(", ");
                        }
                    }

                    queryString = queryString.concat(") ORDER BY " + m + ".name) i");
                    queryString = queryString.concat(" LEFT JOIN (");
                    queryString = queryString.concat("SELECT " + m + ".id, " + m + "_values.values FROM " + m + ", " + m + "_values");
                    queryString = queryString.concat(" WHERE " + m + ".id = " + m + "_values." + m + "_id) v");
                    queryString = queryString.concat(" ON i.id = v.id");
                    queryString = queryString.concat(" GROUP BY i.resource_id, i.name");
                    queryString = queryString.concat(" ORDER BY i.resource_id, i.name");
                    queryString = queryString.concat("') AS output_tbl(");
                    queryString = queryString.concat("id varchar(255)");

                    // define output table schema
                    iter = multiVal_indexMap.get(m).iterator();
                    while (iter.hasNext()) {
                        queryString = queryString.concat(", " + iter.next() + " " + dataTypeMap.get(m) + "[]");
                    }
                    queryString = queryString.concat(") ) m_" + m + " USING(id)");
                }
            }
            queryString = queryString.concat(")");

            Query query = getEntityManager().createNativeQuery(queryString);
            try {
                logger.info(queryString);
                getEntityManager().joinTransaction();
                query.executeUpdate();
            } catch (Exception e) {
                logger.info("View was not created", e);
            }
        }
    }

    @Override
    public void deleteView(String resourceType) {
        logger.info("Deleting view ");
        getEntityManager().joinTransaction();
        getEntityManager().createNativeQuery("DROP VIEW IF EXISTS " + resourceType + "_view").executeUpdate();
        getEntityManager().flush();
    }


}
