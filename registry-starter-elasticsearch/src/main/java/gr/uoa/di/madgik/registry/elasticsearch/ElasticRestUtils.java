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

package gr.uoa.di.madgik.registry.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Small adapter layer around Elasticsearch's low-level REST client.
 *
 * <p>The Elasticsearch module still exposes the existing service interfaces, but the
 * implementation now talks to Elasticsearch 8 via raw HTTP requests. Centralizing request
 * construction and JSON parsing here keeps the indexing and search services focused on query
 * semantics rather than transport details.</p>
 */
public final class ElasticRestUtils {

    private ElasticRestUtils() {
    }

    /**
     * Executes an HTTP request whose response body is expected to contain JSON.
     */
    public static JsonNode performJsonRequest(RestClient client, ObjectMapper objectMapper, String method,
                                              String endpoint, Map<String, String> params, String body) {
        try {
            Request request = new Request(method, endpoint);
            if (params != null && !params.isEmpty()) {
                request.addParameters(params);
            }
            if (body != null) {
                request.setJsonEntity(body);
            }
            return parseEntity(objectMapper, client.performRequest(request));
        } catch (IOException e) {
            throw new ServiceException("Elasticsearch request failed: " + method + " " + endpoint, e);
        }
    }

    public static JsonNode performJsonRequest(RestClient client, ObjectMapper objectMapper, String method,
                                              String endpoint, String body) {
        return performJsonRequest(client, objectMapper, method, endpoint, Map.of(), body);
    }

    /**
     * Executes a request without imposing any response parsing strategy.
     */
    public static Response performRequest(RestClient client, String method, String endpoint,
                                          Map<String, String> params, HttpEntity entity) throws IOException {
        Request request = new Request(method, endpoint);
        if (params != null && !params.isEmpty()) {
            request.addParameters(params);
        }
        if (entity != null) {
            request.setEntity(entity);
        }
        return client.performRequest(request);
    }

    /**
     * Sends an NDJSON payload, used by Elasticsearch bulk APIs.
     */
    public static Response performNdjsonRequest(RestClient client, String method, String endpoint,
                                                Map<String, String> params, String body) throws IOException {
        return performRequest(client, method, endpoint, params,
                new StringEntity(body, ContentType.create("application/x-ndjson", StandardCharsets.UTF_8)));
    }

    /**
     * Parses a REST response entity into a Jackson tree, returning {@code nullNode()} for empty bodies.
     */
    public static JsonNode parseEntity(ObjectMapper objectMapper, Response response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return objectMapper.nullNode();
        }
        String content = EntityUtils.toString(entity);
        if (content == null || content.isBlank()) {
            return objectMapper.nullNode();
        }
        return objectMapper.readTree(content);
    }

    /**
     * Returns {@code true} when the low-level client failure maps to HTTP 404.
     */
    public static boolean isNotFound(ResponseException e) {
        return e.getResponse().getStatusLine().getStatusCode() == 404;
    }
}
