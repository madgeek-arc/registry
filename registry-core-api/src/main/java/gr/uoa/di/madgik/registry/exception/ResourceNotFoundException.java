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
 * Thrown when a resource could not be found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a ResourceNotFoundException with default message.
     */
    public ResourceNotFoundException() {
        super("Resource Not Found");
    }

    /**
     * Constructs a ResourceNotFoundException displaying the searched id.
     */
    public ResourceNotFoundException(String id) {
        super(String.format("Resource with [id=%s] was not found", id));
    }

    /**
     * Constructs a ResourceNotFoundException displaying the resourceType and the searched id.
     */
    public ResourceNotFoundException(String id, String resourceType) {
        super(String.format("[resourceType=%s] with [id=%s] was not found", resourceType, id));
    }

    /**
     * Constructs a ResourceNotFoundException displaying a custom message.
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
