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

import gr.uoa.di.madgik.registry.domain.Version;

import java.util.List;

public interface VersionService {

    Version getVersion(String resource_id, String version);

    List<Version> getVersionsByResource(String resource_id);

    List<Version> getVersionsByResourceType(String resourceType_name);

    List<Version> getAllVersions();

    /**
     * Overwrites the payload of an already-persisted {@link Version}, in place.
     * <p>
     * Versions are otherwise an immutable audit trail — this exists solely for rewriting
     * historical payloads to comply with erasure obligations (e.g. GDPR right-to-erasure),
     * where PII must be scrubbed from past revisions of a resource, not just its current one.
     * It is intentionally not reachable over REST, and not implemented by the REST-client-backed
     * {@code VersionService}: callers must be embedding the registry directly against its own
     * persistence unit.
     *
     * @param version a {@link Version} obtained from this service, with its payload already
     *                mutated by the caller
     * @return the persisted {@link Version}
     */
    Version updateVersion(Version version);

}

