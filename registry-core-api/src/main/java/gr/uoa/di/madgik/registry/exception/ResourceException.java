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

/**
 * General resource exception.
 */
public class ResourceException extends RuntimeException {
    private HttpStatus status;

    /**
     * Constructs a ResourceException having the specified {@link HttpStatus status} code.
     */
    public ResourceException(HttpStatus status) {
        this("ResourceException", status);
    }

    /**
     * Constructs a ResourceException having a custom message and the specified {@link HttpStatus status} code.
     */
    public ResourceException(String msg, HttpStatus status) {
        super(msg);
        this.setStatus(status);
    }

    /**
     * Constructs a ResourceException from an existing {@link Exception} having the specified {@link HttpStatus status} code.
     */
    public ResourceException(Exception e, HttpStatus status) {
        super(e);
        this.setStatus(status);
    }

    public HttpStatus getStatus() {
        return status;
    }

    private void setStatus(HttpStatus status) {
        this.status = status;
    }
}
