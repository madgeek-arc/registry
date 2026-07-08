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

package gr.uoa.di.madgik.registry.encodedslash;

import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ViewService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real embedded-server test (not MockMvc, which never touches the actual Tomcat connector)
 * proving the full production autoconfiguration chain — Tomcat connector relaxation, the
 * {@code encodedSlashHttpFirewallCustomizer} WebSecurityCustomizer, and GenericController
 * itself — works end-to-end for a URL-encoded slash in {@code {id}} when
 * {@code allow-encoded-slash=true}. See the disabled-flag sibling test for the negative case.
 */
@SpringBootTest(
        classes = GenericControllerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "registry.rest.generic-controller.allow-encoded-slash=true"
        })
// Class-level @Sql on a subclass overrides (does not merge with) the superclass's declaration,
// so the full script list — including the base fixtures the superclass would otherwise load — is
// repeated here explicitly, with the encoded-slash fixture appended last.
@Sql(scripts = {"/resource_chunk.sql", "/data.sql", "/employee_slash_fixture.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class GenericControllerEncodedSlashIdIntegrationTest extends PostgreSqlTestContainerSupport {

    @LocalServerPort
    private int port;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @Autowired
    private ResourceTypeService resourceTypeService;

    @Autowired
    private ViewService viewService;

    @Test
    void encodedSlashInRecordsIdReachesGenericControllerWhenEnabled() throws Exception {
        // ResourceTypeViewInit only builds SQL views for resource types that exist at
        // application startup; "employee_slash" is inserted afterward by @Sql, so its view has
        // to be created explicitly here too — same pattern DefaultSearchServiceQueryBuilderTest uses.
        viewService.createView(resourceTypeService.getResourceType("employee_slash"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/records/employee_slash/idPrefix%2FidSuffix"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("idPrefix/idSuffix"),
                "expected the resolved employee_slash resource to contain the decoded id, got: " + response.body());
    }
}
