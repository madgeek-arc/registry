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
import gr.uoa.di.madgik.registry.testsupport.GenericControllerTestApplication;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Embedded-server coverage of {@link GenericController}'s {@code /key} route family, which
 * exists specifically for resource types with a composite primary key (none of the fixtures used
 * elsewhere have more than one {@code primarykey=true} field).
 *
 * <p>{@code /key/recommendations} is only exercised for its 400 (bad key) short-circuit here —
 * its happy path needs a real, correctly-stubbed embedding pipeline
 * ({@code WeightedSegmentEmbeddingService} calls straight into the injected
 * {@code EmbeddingModel}), which is out of scope for this route-wiring test; see
 * {@code DefaultSearchServiceSemanticTest} for embedding-level recommend coverage.
 */
@SpringBootTest(
        classes = GenericControllerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
// Class-level @Sql on a subclass overrides (does not merge with) the superclass's declaration,
// so the full script list — including the base fixtures the superclass would otherwise load — is
// repeated here explicitly, with the composite-key fixture appended last.
@Sql(scripts = {"/resource_chunk.sql", "/data.sql", "/generic_controller_composite_key_fixture.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class GenericControllerCompositeKeyIntegrationTest extends PostgreSqlTestContainerSupport {

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
    void createGadgetView() {
        // ResourceTypeViewInit only builds SQL views for resource types that exist at
        // application startup; "gadget" is inserted afterward by @Sql, so its view has to be
        // created explicitly here too — same pattern the widget/encoded-slash tests use.
        viewService.createView(resourceTypeService.getResourceType("gadget"));
    }

    @Test
    void createGetUpdateDeleteRoundTripByKey() throws Exception {
        HttpResponse<String> created = send("POST", "/records/gadget", "{\"vendor\":\"acme\",\"sku\":\"X1\",\"label\":\"Gadget One\"}");
        assertEquals(201, created.statusCode());
        assertEquals("Gadget One", readTree(created).get("label").asString());
        URI location = URI.create(created.headers().firstValue("Location").orElseThrow());
        assertEquals("/records/gadget/key", location.getPath());
        assertTrue(location.getQuery().contains("vendor=acme"));
        assertTrue(location.getQuery().contains("sku=X1"));

        HttpResponse<String> duplicate = send("POST", "/records/gadget", "{\"vendor\":\"acme\",\"sku\":\"X1\",\"label\":\"Duplicate\"}");
        assertEquals(409, duplicate.statusCode());

        HttpResponse<String> fetched = send("GET", "/records/gadget/key?vendor=acme&sku=X1", null);
        assertEquals(200, fetched.statusCode());
        assertEquals("Gadget One", readTree(fetched).get("label").asString());

        // PUT never needed a /key variant: identity is always derived from the body, single or
        // composite key alike.
        HttpResponse<String> updated = send("PUT", "/records/gadget", "{\"vendor\":\"acme\",\"sku\":\"X1\",\"label\":\"Gadget One Updated\"}");
        assertEquals(200, updated.statusCode());
        assertEquals("Gadget One Updated", readTree(updated).get("label").asString());

        HttpResponse<String> versions = send("GET", "/records/gadget/key/versions?vendor=acme&sku=X1", null);
        assertEquals(200, versions.statusCode());
        JsonNode versionsList = readTree(versions);
        assertTrue(versionsList.isArray());
        assertTrue(versionsList.size() >= 1);
        String versionLabel = versionsList.get(0).get("version").asString();

        HttpResponse<String> version = send("GET",
                "/records/gadget/key/versions/" + versionLabel + "?vendor=acme&sku=X1", null);
        assertEquals(200, version.statusCode());

        HttpResponse<String> deleted = send("DELETE", "/records/gadget/key?vendor=acme&sku=X1", null);
        assertEquals(200, deleted.statusCode());

        HttpResponse<String> afterDelete = send("GET", "/records/gadget/key?vendor=acme&sku=X1", null);
        assertEquals(404, afterDelete.statusCode());
    }

    @Test
    void getByKeyWithMissingFieldReturnsBadRequest() throws Exception {
        HttpResponse<String> response = send("GET", "/records/gadget/key?vendor=acme", null);
        assertEquals(400, response.statusCode());
    }

    @Test
    void getByKeyWithUnexpectedFieldReturnsBadRequest() throws Exception {
        HttpResponse<String> response = send("GET", "/records/gadget/key?vendor=acme&sku=X1&extra=foo", null);
        assertEquals(400, response.statusCode());
    }

    @Test
    void deleteByKeyWithMissingFieldReturnsBadRequest() throws Exception {
        HttpResponse<String> response = send("DELETE", "/records/gadget/key?sku=X1", null);
        assertEquals(400, response.statusCode());
    }

    @Test
    void recommendByKeyWithMissingFieldReturnsBadRequest() throws Exception {
        HttpResponse<String> response = send("GET", "/records/gadget/key/recommendations?vendor=acme", null);
        assertEquals(400, response.statusCode());
        assertFalse(response.body().isBlank());
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
