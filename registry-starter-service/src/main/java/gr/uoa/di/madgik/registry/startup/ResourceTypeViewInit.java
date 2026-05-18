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

package gr.uoa.di.madgik.registry.startup;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ResourceTypeViewInit implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeViewInit.class);

    private final ResourceTypeService resourceTypeService;
    private final ViewService viewService;
    private final NamedParameterJdbcOperations jdbcTemplate;

    @Autowired
    public ResourceTypeViewInit(ResourceTypeService resourceTypeService,
                                ViewService viewService,
                                @Qualifier("registryDataSource") DataSource dataSource) {
        this(resourceTypeService, viewService, new NamedParameterJdbcTemplate(dataSource));
    }

    ResourceTypeViewInit(ResourceTypeService resourceTypeService,
                         ViewService viewService,
                         NamedParameterJdbcOperations jdbcTemplate) {
        this.resourceTypeService = resourceTypeService;
        this.viewService = viewService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ResourceType> resourceTypes = resourceTypeService.getAllResourceType();
        int created = 0;
        for (ResourceType resourceType : resourceTypes) {
            if (!viewExists(resourceType.getName())) {
                logger.info("Creating missing view for resource type '{}'", resourceType.getName());
                viewService.createView(resourceType);
                created++;
            }
        }
        if (created > 0) {
            logger.info("Created {} missing resource type views", created);
        }
    }

    private boolean viewExists(String resourceTypeName) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.views
                    WHERE table_schema = 'public'
                      AND table_name = :viewName
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("viewName", resourceTypeName + "_view"),
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }
}
