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

package gr.uoa.di.madgik.registry.controllers.advice;

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resources/service");

    @Test
    void registryNotFoundIsMappedTo404() {
        ResponseEntity<ProblemDetail> response = handler.handleNotFound(new ResourceNotFoundException("r1", "service"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/resources/service");
    }

    @Test
    void serviceExceptionIsMappedTo400() {
        ResponseEntity<ProblemDetail> response = handler.handleServiceException(new ServiceException("bad"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getDetail()).isEqualTo("bad");
    }

    @Test
    void unknownPathKeeps404() {
        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(
                new NoResourceFoundException(HttpMethod.GET, "/missing", "missing"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void unsupportedMethodKeeps405() {
        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(
                new HttpRequestMethodNotSupportedException("PATCH"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(405);
    }

    @Test
    void otherExceptionsAreReportedAs500WithoutTheirMessage() {
        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(new IllegalStateException("secret detail"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
    }
}
