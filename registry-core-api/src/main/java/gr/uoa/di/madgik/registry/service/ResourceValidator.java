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

/**
 * Strategy interface for validating registry resources before they are persisted.
 *
 * <p>Implementations perform domain-specific validation (e.g. mandatory field checks, format
 * checks, vocabulary lookups) and either return the (possibly normalised) resource on success,
 * or throw an unchecked exception on failure.
 *
 * <h2>Registration</h2>
 * <p>Declare an implementation as a Spring bean and it will be picked up automatically by
 * {@link GenericResourceService}. If no bean is present, validation is skipped and resources
 * are persisted as-is.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Service
 * public class MyValidator implements ResourceValidator {
 *
 *     @Override
 *     public <T> T validate(T resource, String resourceTypeName) {
 *         // perform checks, throw if invalid
 *         return resource;
 *     }
 * }
 * }</pre>
 *
 * @see GenericResourceService#validate(String, Object)
 */
public interface ResourceValidator {

    /**
     * Validates {@code resource} against the rules defined for {@code resourceTypeName}.
     *
     * <p>Implementations may modify the resource (e.g. normalise values, strip unknown fields)
     * and must return the final object that should be persisted. If validation fails the
     * implementation must throw an unchecked exception; the specific type is left to the
     * implementation (e.g. {@code ValidationException}, {@code IllegalArgumentException}).
     *
     * @param resource         the domain object to validate; never {@code null}
     * @param resourceTypeName the name of the {@code ResourceType} whose rules apply
     * @param <T>              the domain type
     * @return the validated (and optionally normalised) resource, never {@code null}
     * @throws RuntimeException if validation fails
     */
    <T> T validate(T resource, String resourceTypeName);
}
