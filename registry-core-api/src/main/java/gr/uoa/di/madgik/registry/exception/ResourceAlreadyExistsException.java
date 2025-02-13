/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when attempting to create a resource using an existing id.
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a ResourceAlreadyExistsException with default message.
     */
    public ResourceAlreadyExistsException() {
        super("Resource Already Exists");
    }

    /**
     * Constructs a ResourceAlreadyExistsException displaying the conflicting id.
     */
    public ResourceAlreadyExistsException(String id) {
        super(String.format("Resource with [id=%s] already exists", id));
    }

    /**
     * Constructs a ResourceAlreadyExistsException displaying the resourceType and the conflicting id.
     */
    public ResourceAlreadyExistsException(String id, String resourceType) {
        super(String.format("[resourceType=%s] with [id=%s] already exists", resourceType, id));
    }
}
