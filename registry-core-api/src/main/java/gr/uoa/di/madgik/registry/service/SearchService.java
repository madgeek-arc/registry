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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.List;
import java.util.Map;


public interface SearchService {

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType, int quantity, int from, String sortByField, String sortOrder);

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> cqlQuery(String query, String resourceType);

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> search(FacetFilter filter) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException;

    @Retryable(value = ServiceException.class, maxAttempts = 2, backoff = @Backoff(value = 200))
    Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category);

    class KeyValue {

        public String field;

        public String value;

        public KeyValue(String field, String value) {
            this.field = field;
            this.value = value;
        }

        public KeyValue() {
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}