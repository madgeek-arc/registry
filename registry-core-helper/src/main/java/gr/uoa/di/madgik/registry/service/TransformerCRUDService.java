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

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.springframework.security.core.Authentication;

public interface TransformerCRUDService<T, R, U extends Authentication> {
    /**
     * Returns the resource.
     *
     * @param id of the resource in the index.
     * @return the resource.
     */
    R get(String id);

    /**
     * Returns all resource.
     *
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing<R> getAll(FacetFilter filter, U authentication);

    /**
     * Returns all resource of a user.
     *
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing<R> getMy(FacetFilter filter, U authentication);

    /**
     * Add a new resource.
     *
     * @param resource to be added
     * @return the id of the inserted resource.
     */
    R add(T resource, U authentication);

    /**
     * Update the resource.
     *
     * @param resource to be updated.
     * @return the updated resource.
     */
    R update(T resource, U authentication);

    /**
     * Delete the resource.
     *
     * @param resourceId to be deleted.
     */
    void delete(T resourceId);
}
