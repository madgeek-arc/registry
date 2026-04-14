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

import gr.uoa.di.madgik.registry.dao.ViewDao;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Service("viewService")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class ViewServiceImpl implements ViewService, ResourceTypeProjectionService {

    private final ViewDao viewDao;
    private final ResourceTypeService resourceTypeService;
    private final DataSource dataSource;

    public ViewServiceImpl(ViewDao viewDao,
                           ResourceTypeService resourceTypeService,
                           @Qualifier("registryDataSource") DataSource dataSource) {
        this.viewDao = viewDao;
        this.resourceTypeService = resourceTypeService;
        this.dataSource = dataSource;
    }

    @Override
    public void createView(ResourceType resourceType) {
        viewDao.createView(resourceType);
    }

    @Override
    public void deleteView(String resourceType) {
        viewDao.deleteView(resourceType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> fetchResourceIds(String resourceType) {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        String query = "SELECT id FROM " + resourceType + "_view";

        List<Map<String, Object>> records;
        try {
            records = jdbcTemplate.queryForList(query, new MapSqlParameterSource());
        } catch (BadSqlGrammarException e) {
            if (!isRecoverableViewFailure(e)) {
                throw e;
            }
            recreateView(resourceType);
            records = jdbcTemplate.queryForList(query, new MapSqlParameterSource());
        }

        return records.stream()
                .map(record -> (String) record.get("id"))
                .toList();
    }

    private boolean isRecoverableViewFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlEx) {
                if ("42P01".equals(sqlEx.getSQLState())) {
                    return true;
                }
                String message = sqlEx.getMessage();
                if (message != null && message.contains("return and sql tuple descriptions are incompatible")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void recreateView(String resourceType) {
        ResourceType resourceTypeDefinition = resourceTypeService.getResourceType(resourceType);
        if (resourceTypeDefinition == null) {
            throw new ServiceException("Cannot recreate missing view for unknown resource type " + resourceType);
        }
        deleteView(resourceType);
        createView(resourceTypeDefinition);
    }

}
