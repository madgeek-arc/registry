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

package gr.uoa.di.madgik.registry.controllers;

import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.ViewService;
import gr.uoa.di.madgik.registry.testsupport.TypedResourceControllerTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * First real embedded-server (not MockMvc) coverage of {@link TypedResourceController}'s core CRUD
 * flow, and of the {@code registry-core-rest} error handling wired up in
 * {@code GlobalExceptionHandler}. Bootstraps the test infrastructure that later
 * duplicate-key-detection and composite-primary-key tests build on.
 */
@SpringBootTest(
        classes = TypedResourceControllerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
// Class-level @Sql on a subclass overrides (does not merge with) the superclass's declaration,
// so the full script list — including the base fixtures the superclass would otherwise load — is
// repeated here explicitly, with the widget fixture appended last.
@Sql(scripts = {"/resource_chunk.sql", "/data.sql", "/typed_resource_controller_crud_fixture.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class TypedResourceControllerCrudIntegrationTest extends PostgreSqlTestContainerSupport {

    @LocalServerPort
    private int port;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @Autowired
    private ResourceTypeService resourceTypeService;

    @Autowired
    private ViewService viewService;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void createWidgetView() {
        // ResourceTypeViewInit only builds SQL views for resource types that exist at
        // application startup; "widget" is inserted afterward by @Sql, so its view has to be
        // created explicitly here too — same pattern the encoded-slash tests use.
        viewService.createView(resourceTypeService.getResourceType("widget"));
    }

    @Test
    void createGetUpdateDeleteRoundTrip() throws Exception {
        HttpResponse<String> created = send("POST", "/records/widget", "{\"code\":\"W-1\",\"label\":\"Widget One\"}");
        assertEquals(201, created.statusCode());
        assertEquals("Widget One", readTree(created).get("label").asString());
        assertEquals("http://localhost:" + port + "/records/widget/W-1",
                created.headers().firstValue("Location").orElse(null));

        HttpResponse<String> duplicate = send("POST", "/records/widget", "{\"code\":\"W-1\",\"label\":\"Widget One Duplicate\"}");
        assertEquals(409, duplicate.statusCode());
        assertEquals(409, readTree(duplicate).get("status").asInt());

        HttpResponse<String> fetched = send("GET", "/records/widget/W-1", null);
        assertEquals(200, fetched.statusCode());
        assertEquals("Widget One", readTree(fetched).get("label").asString());

        HttpResponse<String> updated = send("PUT", "/records/widget", "{\"code\":\"W-1\",\"label\":\"Widget One Updated\"}");
        assertEquals(200, updated.statusCode());
        assertEquals("Widget One Updated", readTree(updated).get("label").asString());

        HttpResponse<String> refetched = send("GET", "/records/widget/W-1", null);
        assertEquals(200, refetched.statusCode());
        assertEquals("Widget One Updated", readTree(refetched).get("label").asString());

        HttpResponse<String> deleted = send("DELETE", "/records/widget/W-1", null);
        assertEquals(200, deleted.statusCode());

        HttpResponse<String> afterDelete = send("GET", "/records/widget/W-1", null);
        assertEquals(404, afterDelete.statusCode());
        assertEquals(404, readTree(afterDelete).get("status").asInt());
    }

    @Test
    void createOnUnknownResourceTypeReturnsNotFoundInsteadOfServerError() throws Exception {
        HttpResponse<String> response = send("POST", "/records/does-not-exist", "{\"code\":\"W-1\"}");

        assertEquals(404, response.statusCode());
        assertEquals(404, readTree(response).get("status").asInt());
    }

    /**
     * Regression coverage for GenericResourceManager's alias resolution: CRUD/version routes
     * previously 404'd on a resource-type alias ("widget-alias", declared in the fixture SQL)
     * even though the browse endpoint already resolved it — see GenericResourceManager.resolveResourceType.
     */
    @Test
    void aliasRoundTripAcrossCrudAndVersionRoutes() throws Exception {
        HttpResponse<String> created = send("POST", "/records/widget-alias", "{\"code\":\"W-2\",\"label\":\"Widget Two\"}");
        assertEquals(201, created.statusCode());
        assertEquals("Widget Two", readTree(created).get("label").asString());

        HttpResponse<String> fetchedById = send("GET", "/records/widget-alias/W-2", null);
        assertEquals(200, fetchedById.statusCode());
        assertEquals("Widget Two", readTree(fetchedById).get("label").asString());

        HttpResponse<String> fetchedByKey = send("GET", "/records/widget-alias/key?code=W-2", null);
        assertEquals(200, fetchedByKey.statusCode());
        assertEquals("Widget Two", readTree(fetchedByKey).get("label").asString());

        HttpResponse<String> versions = send("GET", "/records/widget-alias/key/versions?code=W-2", null);
        assertEquals(200, versions.statusCode());
        assertEquals(1, readTree(versions).size());

        HttpResponse<String> updated = send("PUT", "/records/widget-alias", "{\"code\":\"W-2\",\"label\":\"Widget Two Updated\"}");
        assertEquals(200, updated.statusCode());
        assertEquals("Widget Two Updated", readTree(updated).get("label").asString());

        HttpResponse<String> deleted = send("DELETE", "/records/widget-alias/W-2", null);
        assertEquals(200, deleted.statusCode());

        HttpResponse<String> afterDelete = send("GET", "/records/widget-alias/W-2", null);
        assertEquals(404, afterDelete.statusCode());
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode readTree(HttpResponse<String> response) {
        return jsonMapper.readTree(response.body());
    }
}
