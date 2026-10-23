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

import gr.uoa.di.madgik.registry.dao.ResourceDao;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.dao.VersionDao;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.resourcesync.domain.ResourceSyncEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceSyncServiceImplTest {

    private final ResourceTypeDao resourceTypeDao = mock(ResourceTypeDao.class);

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void capabilityListUsesConfiguredHostWithoutTrailingSlash() {
        ResourceSyncServiceImpl service = service(new MockEnvironment().withProperty("registry.host", "https://registry.example.org/api/"));

        assertThat(capabilityUrls(service)).containsExactly(
                "https://registry.example.org/api/resourcesync/service/resourcelist.xml");
    }

    @Test
    void capabilityListFallsBackToCurrentRequestWhenHostIsUnset() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resourcesync/");
        request.setScheme("https");
        request.setServerName("public.example.org");
        request.setServerPort(443);
        request.setContextPath("/api");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ResourceSyncServiceImpl service = service(new MockEnvironment());

        assertThat(capabilityUrls(service)).containsExactly(
                "https://public.example.org/api/resourcesync/service/resourcelist.xml");
    }

    private ResourceSyncServiceImpl service(MockEnvironment environment) {
        ResourceType type = new ResourceType();
        type.setName("service");
        when(resourceTypeDao.getAllResourceType()).thenReturn(List.of(type));
        return new ResourceSyncServiceImpl(environment, resourceTypeDao, mock(ResourceDao.class), mock(VersionDao.class));
    }

    private static List<String> capabilityUrls(ResourceSyncServiceImpl service) {
        return service.getCapabilityList().getEntries().stream()
                .map(ResourceSyncEntry::getLoc)
                .filter(loc -> loc.endsWith("resourcelist.xml"))
                .toList();
    }
}
