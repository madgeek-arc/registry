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

import gr.uoa.di.madgik.registry.domain.Resource;

import java.util.Arrays;

/**
 * Converts registry {@link Resource} payloads between their string representation and typed
 * Java objects.
 *
 * <p>The registry persists resource payloads as raw strings in one of two formats:
 * {@code "json"} or {@code "xml"}. This service abstracts the serialization mechanism so that
 * callers (e.g. {@link GenericResourceService}) do not need to know which format a given
 * resource uses.
 *
 * <h2>Supported payload formats</h2>
 * <table border="1">
 *   <caption>Format support matrix</caption>
 *   <tr>
 *     <th>Format</th>
 *     <th>Backing library</th>
 *     <th>Domain class requirements</th>
 *   </tr>
 *   <tr>
 *     <td>{@code json}</td>
 *     <td>Jackson ({@code ObjectMapper})</td>
 *     <td>Any POJO that Jackson can handle. Jackson annotations ({@code @JsonProperty}, etc.)
 *         are optional but recommended. No extra Spring configuration needed.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code xml}</td>
 *     <td>JAXB ({@code JAXBContext})</td>
 *     <td>
 *       The class <strong>must</strong>:<br>
 *       1. Be annotated with {@code @XmlRootElement} (or reachable via {@code @XmlSeeAlso}).<br>
 *       2. Be registered in the {@code JAXBContext} Spring bean at application startup.<br>
 *       When using the catalogue library, registration is done by adding the class's package
 *       to the {@code catalogue-lib.jaxb.include-packages} configuration property.
 *     </td>
 *   </tr>
 * </table>
 *
 * <h2>Format is determined by the ResourceType</h2>
 * <p>The payload format is stored per-resource in {@link Resource#getPayloadFormat()} and is
 * set when the {@code ResourceType} is registered (via its {@code payloadType} field). All
 * resources of a given type use the same format. Mixing formats within a type is not supported.
 *
 * @see ParserPool
 * @see GenericResourceService
 */
public interface ParserService {

    /**
     * Deserializes the payload of {@code resource} into an instance of {@code returnType}.
     *
     * <p>The format is read from {@link Resource#getPayloadFormat()}.
     *
     * @param resource   the registry resource whose payload is to be deserialized;
     *                   must not be {@code null}
     * @param returnType the target Java class; for XML payloads this class must be registered
     *                   in the {@code JAXBContext} bean
     * @param <T>        the target type
     * @return the deserialized domain object, never {@code null}
     * @throws ServiceException if the resource is {@code null}, if the format is unsupported,
     *                          if parsing fails, or (for XML) if the class is not registered
     *                          in the {@code JAXBContext}
     */
    <T> T deserialize(Resource resource, Class<T> returnType);

    /**
     * Serializes {@code resource} to a string in the given {@code mediaType}.
     *
     * @param resource  the domain object to serialize; must not be {@code null}
     * @param mediaType the target format
     * @return the serialized string representation
     * @throws ServiceException if serialization fails or (for XML) if the class is not
     *                          registered in the {@code JAXBContext}
     */
    String serialize(Object resource, ParserServiceTypes mediaType);

    /**
     * Extracts a single string value from {@code payload} using a format-specific path expression.
     *
     * <p>For {@code "json"} payloads the {@code path} is a JSONPath expression (e.g. {@code $.id}).
     * For {@code "xml"} payloads the {@code path} is an XPath expression.
     *
     * @param payload     the raw serialized payload string
     * @param payloadType the format of the payload ({@code "json"} or {@code "xml"})
     * @param path        the field path expression
     * @return the extracted string value, or {@code null} if the path resolves to nothing
     * @throws ServiceException if the payload cannot be parsed or the format is unsupported
     */
    String extractValue(String payload, String payloadType, String path);

    /**
     * The payload formats supported by the registry.
     */
    enum ParserServiceTypes {
        JSON("json"),
        XML("xml");

        private final String type;

        ParserServiceTypes(final String type) {
            this.type = type;
        }

        /**
         * Returns the {@code ParserServiceTypes} constant whose key matches {@code s}
         * (case-insensitive).
         *
         * @param s the format string (e.g. {@code "json"} or {@code "xml"})
         * @return the matching constant
         * @throws IllegalArgumentException if no constant matches
         */
        public static ParserServiceTypes fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(ParserServiceTypes.values())
                    .filter(v -> v.type.equalsIgnoreCase(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown payload format '" + s + "'. Supported values: 'json', 'xml'."));
        }

        public String getKey() {
            return type;
        }
    }
}
