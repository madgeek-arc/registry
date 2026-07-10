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

import gr.uoa.di.madgik.registry.exception.MissingResourceEmbeddingsException;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.exception.UnsupportedSearchParameterException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Library-default mapping from the exceptions thrown by registry services to RFC 7807
 * {@link ProblemDetail} bodies, so any application embedding {@code registry-core-rest}
 * controllers (e.g. the standalone {@code registry-service} app) gets a consistent error
 * response shape out of the box, instead of each embedding app's default error page.
 *
 * <p>Registered at {@link Ordered#LOWEST_PRECEDENCE} deliberately: this is a fallback. An
 * embedding application that supplies its own {@code @ControllerAdvice} (e.g. catalogue's
 * {@code GenericExceptionController}, which already handles {@link ResourceException},
 * {@link ResourceAlreadyExistsException}, and {@link ResourceNotFoundException} with its own
 * {@link ProblemDetail} conventions) should declare an explicit, higher-precedence
 * {@code @Order} on its own advice to take priority for exception types both advices handle.
 * Without an explicit order on both sides, Spring resolves per-advice-bean and does not warn
 * on the overlap, so the winner would otherwise depend on undefined bean registration order.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleAlreadyExists(ResourceAlreadyExistsException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedSearchParameterException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedSearchParameter(UnsupportedSearchParameterException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(MissingResourceEmbeddingsException.class)
    public ResponseEntity<ProblemDetail> handleMissingEmbeddings(MissingResourceEmbeddingsException e, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage(), request);
    }

    @ExceptionHandler(ResourceException.class)
    public ResponseEntity<ProblemDetail> handleResourceException(ResourceException e, HttpServletRequest request) {
        return build(e.getStatus(), e.getMessage(), request);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ProblemDetail> handleServiceException(ServiceException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest request) {
        logger.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatusCode status, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        if (status instanceof HttpStatus httpStatus) {
            problemDetail.setTitle(httpStatus.getReasonPhrase());
        }
        problemDetail.setInstance(getUriWithParams(request));
        problemDetail.setProperty("method", request.getMethod());
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(problemDetail);
    }

    private static URI getUriWithParams(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder(request.getRequestURI());
        if (request.getQueryString() != null) {
            sb.append('?').append(request.getQueryString());
        }
        return URI.create(sb.toString());
    }
}
