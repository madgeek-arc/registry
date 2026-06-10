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

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.Resource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Default implementation of {@link ParserService} backed by Jackson (JSON) and JAXB (XML).
 *
 * <h2>JSON support</h2>
 * <p>Deserialization uses {@link tools.jackson.databind.ObjectMapper} and works for any
 * class that Jackson can handle — plain POJOs, classes annotated with {@code @JsonProperty}, etc.
 * No additional configuration is required.
 *
 * <h2>XML support</h2>
 * <p>Serialization and deserialization use the {@link JAXBContext} Spring bean injected at
 * construction time. A domain class must satisfy <strong>both</strong> of the following
 * conditions to be usable with XML payloads:
 * <ol>
 *   <li>The class must be annotated with {@code @XmlRootElement} (or registered via
 *       {@code @XmlSeeAlso} from a class that is).</li>
 *   <li>The class (or its containing package) must be included in the packages scanned when the
 *       {@code JAXBContext} bean is constructed. When using the catalogue library this is
 *       configured via the {@code catalogue-lib.jaxb.include-packages} property.</li>
 * </ol>
 * <p>If these conditions are not met, {@link #serialize} and {@link #deserialize} will throw a
 * {@link ServiceException} with an actionable message explaining what to configure.
 *
 * @see ParserService
 */
@Component
public class ParserPool implements ParserService {

    private final JAXBContext jaxbContext;
    private final ObjectMapper mapper;

    public ParserPool(JAXBContext jaxbContext, ObjectMapper mapper) {
        this.jaxbContext = jaxbContext;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ServiceException if the resource is {@code null}, if the payload format is not
     *                          {@code "json"} or {@code "xml"}, if JSON parsing fails, or if
     *                          the target class is not registered in the {@link JAXBContext}
     *                          (XML only — see class-level documentation for how to register it)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Resource resource, Class<T> returnType) {
        if (resource == null) {
            throw new ServiceException("Cannot deserialize a null resource");
        }
        return switch (resource.getPayloadFormat()) {
            case "xml" -> deserializeXml(resource.getPayload(), returnType);
            case "json" -> deserializeJson(resource.getPayload(), returnType);
            default -> throw new ServiceException(
                    "Unsupported payload format '" + resource.getPayloadFormat() + "'. "
                    + "Supported formats are: 'json', 'xml'.");
        };
    }

    /**
     * {@inheritDoc}
     *
     * @throws ServiceException if the media type is unsupported, if JSON serialization fails,
     *                          or if the object's class is not registered in the
     *                          {@link JAXBContext} (XML only — see class-level documentation
     *                          for how to register it)
     */
    @Override
    public String serialize(Object resource, ParserServiceTypes mediaType) {
        return switch (mediaType) {
            case XML -> serializeXml(resource);
            case JSON -> serializeJson(resource);
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>For JSON, the path must be a JSONPath expression starting with {@code $.}
     * (e.g. {@code $.id}, {@code $.metadata.identifier}). Simple dot-notation is supported;
     * array subscripts are not.
     * For XML, the path must be a valid XPath expression.
     */
    @Override
    public String extractValue(String payload, String payloadType, String path) {
        return switch (payloadType.toLowerCase()) {
            case "json" -> extractJsonValue(payload, path);
            case "xml" -> extractXmlValue(payload, path);
            default -> throw new ServiceException(
                    "Unsupported payload type '" + payloadType + "'. Supported values: 'json', 'xml'.");
        };
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <T> T deserializeXml(String payload, Class<T> returnType) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (T) unmarshaller.unmarshal(new StringReader(payload));
        } catch (JAXBException e) {
            throw new ServiceException(buildXmlErrorMessage("deserialize", returnType.getName()), e);
        }
    }

    private <T> T deserializeJson(String payload, Class<T> returnType) {
        try {
            return mapper.readValue(payload, returnType);
        } catch (JacksonException e) {
            throw new ServiceException(
                    "Failed to deserialize JSON payload into " + returnType.getName()
                    + ": " + e.getOriginalMessage(), e);
        }
    }

    private String serializeXml(Object resource) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter sw = new StringWriter();
            marshaller.marshal(resource, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new ServiceException(buildXmlErrorMessage("serialize", resource.getClass().getName()), e);
        }
    }

    private String serializeJson(Object resource) {
        try {
            return mapper.writeValueAsString(resource);
        } catch (JacksonException e) {
            throw new ServiceException(
                    "Failed to serialize " + resource.getClass().getName()
                    + " to JSON: " + e.getOriginalMessage(), e);
        }
    }

    private String extractJsonValue(String payload, String path) {
        try {
            // Convert JSONPath ($.a.b.c) to JSON Pointer (/a/b/c)
            String pointer = path.startsWith("$.") ? "/" + path.substring(2).replace('.', '/') : path;
            JsonNode node = mapper.readTree(payload).at(pointer);
            return node.isMissingNode() || node.isNull() ? null : node.asText();
        } catch (JacksonException e) {
            throw new ServiceException("Failed to extract JSON value at path '" + path + "': " + e.getOriginalMessage(), e);
        }
    }

    private String extractXmlValue(String payload, String path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(payload)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            String result = xPath.evaluate(path, doc);
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            throw new ServiceException("Failed to extract XML value at path '" + path + "': " + e.getMessage(), e);
        }
    }

    private static String buildXmlErrorMessage(String operation, String className) {
        return String.format(
                "Failed to %s XML payload for class '%s'. "
                + "The class is not registered in the JAXBContext. "
                + "Ensure the class is annotated with @XmlRootElement and its package is included "
                + "in the 'catalogue-lib.jaxb.include-packages' configuration property.",
                operation, className);
    }
}
