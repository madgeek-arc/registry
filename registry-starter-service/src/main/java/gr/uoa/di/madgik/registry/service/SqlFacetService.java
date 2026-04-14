/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetUtils;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Value;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

@Service
class SqlFacetService {

    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final ResourceTypeService resourceTypeService;

    SqlFacetService(@Qualifier("registryDataSource") DataSource dataSource,
                    ResourceTypeService resourceTypeService) {
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.resourceTypeService = resourceTypeService;
    }

    List<Facet> createFacets(List<String> browseBy, List<ResourceType> resourceTypes,
                             List<String> matchedIds) {
        if (matchedIds == null || matchedIds.isEmpty()) {
            return List.of();
        }
        Map<String, IndexField> fieldMap = buildFieldMap(resourceTypes);
        return FacetUtils.createFacets(
                browseBy,
                field -> {
                    IndexField indexField = fieldMap.get(field);
                    return indexField != null ? indexField.getLabel() : null;
                },
                field -> {
                    IndexField indexField = fieldMap.get(field);
                    return indexField != null ? loadFacetValues(indexField, matchedIds) : null;
                }
        );
    }

    private Map<String, IndexField> buildFieldMap(List<ResourceType> resourceTypes) {
        Map<String, IndexField> fieldMap = new LinkedHashMap<>();
        for (ResourceType resourceType : resourceTypes) {
            for (IndexField indexField : resourceTypeService.getResourceTypeIndexFields(resourceType.getName())) {
                fieldMap.putIfAbsent(indexField.getName(), indexField);
            }
        }
        return fieldMap;
    }

    private List<Value> loadFacetValues(IndexField field, List<String> matchedIds) {
        String valueExpression = field.isMultivalued() ? "facet_value" : "view_row.%s".formatted(field.getName());
        String joinExpression = field.isMultivalued()
                ? "CROSS JOIN LATERAL unnest(view_row.%s) AS facet_value ".formatted(field.getName())
                : "";
        String countExpression = field.isMultivalued() ? "COUNT(DISTINCT view_row.id)" : "COUNT(*)";
        String sql = """
                SELECT CAST(%s AS text) AS value, %s AS count
                FROM %s_view view_row
                %s
                WHERE view_row.id IN (:matchedIds) AND %s IS NOT NULL
                GROUP BY %s
                ORDER BY count DESC, value ASC
                """.formatted(
                valueExpression,
                countExpression,
                field.getResourceType().getName(),
                joinExpression,
                valueExpression,
                valueExpression
        );

        Map<String, Object> params = new HashMap<>();
        params.put("matchedIds", matchedIds);
        List<Value> values = npJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Value value = new Value();
            value.setValue(rs.getString("value"));
            value.setCount(rs.getLong("count"));
            return value;
        });
        return FacetUtils.normalizeValues(values);
    }
}
