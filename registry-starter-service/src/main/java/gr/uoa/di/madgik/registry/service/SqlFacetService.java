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

import gr.uoa.di.madgik.registry.dao.SqlIdentifiers;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetUtils;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Value;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

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
        return createFacets(browseBy, resourceTypes, field -> matchedIds, Map.of());
    }

    /**
     * Creates facets over a separate set of matched ids per field. Each selected value is kept in the
     * facet of its field, even when no matched id has it.
     *
     * @param matchedIdsByField resolves the ids of the resources a field's facet counts
     * @param selections        the selected values by field
     */
    List<Facet> createFacets(List<String> browseBy, List<ResourceType> resourceTypes,
                             Function<String, List<String>> matchedIdsByField, Map<String, Object> selections) {
        Map<String, IndexField> fieldMap = buildFieldMap(resourceTypes);
        return FacetUtils.createFacets(
                browseBy,
                field -> {
                    IndexField indexField = fieldMap.get(field);
                    return indexField != null ? indexField.getLabel() : null;
                },
                field -> {
                    IndexField indexField = fieldMap.get(field);
                    return indexField != null
                            ? FacetUtils.includeSelectedValues(
                                    loadFacetValues(indexField, matchedIdsByField.apply(field)), selections.get(field))
                            : null;
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
        if (matchedIds.isEmpty()) {
            return new ArrayList<>();
        }
        String valueExpression = field.isMultivalued() ? "facet_value" : "view_row.%s".formatted(SqlIdentifiers.quoteColumn(field.getName()));
        String joinExpression = field.isMultivalued()
                ? "CROSS JOIN LATERAL unnest(view_row.%s) AS facet_value ".formatted(SqlIdentifiers.quoteColumn(field.getName()))
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
