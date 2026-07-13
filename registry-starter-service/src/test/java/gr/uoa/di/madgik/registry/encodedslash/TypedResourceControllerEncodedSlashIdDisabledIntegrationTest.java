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
import gr.uoa.di.madgik.registry.testsupport.TypedResourceControllerTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sibling of TypedResourceControllerEncodedSlashIdIntegrationTest with allow-encoded-slash left at its
 * unset/false default — proves the property genuinely gates the relaxation (default stays
 * off/safe) rather than the encoded slash simply working regardless of configuration.
 */
@SpringBootTest(
        classes = TypedResourceControllerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
// Class-level @Sql on a subclass overrides (does not merge with) the superclass's declaration,
// so the full script list — including the base fixtures the superclass would otherwise load — is
// repeated here explicitly, with the encoded-slash fixture appended last.
@Sql(scripts = {"/resource_chunk.sql", "/data.sql", "/employee_slash_fixture.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class TypedResourceControllerEncodedSlashIdDisabledIntegrationTest extends PostgreSqlTestContainerSupport {

    @LocalServerPort
    private int port;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @Test
    void encodedSlashInRecordsIdIsRejectedWhenNotEnabled() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/records/employee_slash/idPrefix%2FidSuffix"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }
}
