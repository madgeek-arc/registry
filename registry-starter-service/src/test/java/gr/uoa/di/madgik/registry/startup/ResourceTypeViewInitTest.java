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
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ResourceTypeViewInitTest {

    @Test
    void run_createsOnlyMissingResourceTypeViews() {
        ResourceTypeService resourceTypeService = mock(ResourceTypeService.class);
        ViewService viewService = mock(ViewService.class);
        NamedParameterJdbcOperations jdbcTemplate = mock(NamedParameterJdbcOperations.class);
        ResourceType missing = resourceType("organisation");
        ResourceType existing = resourceType("service");

        when(resourceTypeService.getAllResourceType()).thenReturn(List.of(missing, existing));
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Boolean.class)))
                .thenReturn(false, true);

        ResourceTypeViewInit initializer = new ResourceTypeViewInit(
                resourceTypeService, viewService, jdbcTemplate);

        initializer.run(null);

        verify(viewService).createView(missing);
        verifyNoMoreInteractions(viewService);
    }

    private ResourceType resourceType(String name) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        return resourceType;
    }
}
