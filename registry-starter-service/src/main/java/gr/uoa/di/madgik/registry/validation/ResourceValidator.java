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

package gr.uoa.di.madgik.registry.validation;

import gr.uoa.di.madgik.registry.dao.SchemaDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;


/**
 * Created by stefanos on 30/1/2017.
 */
@Service("resourceValidator")
public class ResourceValidator {

    private static final Logger logger = LoggerFactory.getLogger(ResourceValidator.class);

    private final SchemaDao schemaDao;

    ResourceValidator(SchemaDao schemaDao) {
        this.schemaDao = schemaDao;
    }

    public boolean validateXML(Resource resource) {
        Schema schema;
        try {
            schema = schemaDao.loadXMLSchema(resource.getResourceType());
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(IOUtils.toInputStream(resource.getPayload())));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    public boolean validateJSON(Resource resource) {
        org.everit.json.schema.Schema schema = schemaDao.loadJSONSchema(resource.getResourceType());
        try {
            schema.validate(new JSONObject(resource.getPayload())); // throws a ValidationException if this object is invalid
        } catch (ValidationException e) {
            logger.error("Error validation JSON payload", e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

}
