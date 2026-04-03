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

package gr.uoa.di.madgik.registry.validation;

import gr.uoa.di.madgik.registry.dao.SchemaDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;


/**
 * Validates a {@link Resource}'s payload against its declared XML or JSON schema.
 *
 * <p>The resource must have its {@code resourceType} and {@code payloadFormat} fields set
 * before calling {@link #validate(Resource)}.
 */
@Service
public class ResourceSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(ResourceSchemaValidator.class);

    private final SchemaDao schemaDao;

    ResourceSchemaValidator(SchemaDao schemaDao) {
        this.schemaDao = schemaDao;
    }

    /**
     * Validates {@code resource.payload} against the schema declared in its {@code ResourceType}.
     *
     * @throws IllegalStateException if {@code resourceType} is not set on the resource
     * @throws ServiceException      if the payload format does not match the resource type's schema type,
     *                               or the format is unsupported
     * @throws ResourceException     if the payload fails schema validation
     */
    public void validate(Resource resource) {
        if (resource.getResourceType() == null) {
            throw new IllegalStateException("Resource must have resourceType set before validation");
        }
        String format = resource.getPayloadFormat().toLowerCase();
        if (!resource.getResourceType().getPayloadType().equalsIgnoreCase(format)) {
            throw new ServiceException("payload and schema format are different");
        }
        switch (format) {
            case "xml" -> validateXML(resource);
            case "json" -> validateJSON(resource);
            default -> throw new ServiceException("Unsupported payload format: " + format);
        }
    }

    private void validateXML(Resource resource) {
        try {
            Schema schema = schemaDao.loadXMLSchema(resource.getResourceType());
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(IOUtils.toInputStream(resource.getPayload())));
        } catch (Exception e) {
            throw new ResourceException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateJSON(Resource resource) {
        org.everit.json.schema.Schema schema = schemaDao.loadJSONSchema(resource.getResourceType());
        try {
            schema.validate(new JSONObject(resource.getPayload()));
        } catch (ValidationException e) {
            logger.error("Error validating JSON payload", e);
            throw new ResourceException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
