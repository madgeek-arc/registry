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

package gr.uoa.di.madgik.registry.monitor;

import gr.uoa.di.madgik.registry.dao.SchemaDao;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchemaListener implements ResourceTypeListener {

    private static final Logger logger = LoggerFactory.getLogger(SchemaListener.class);

    private final SchemaDao schemaDao;

    public SchemaListener(SchemaDao schemaDao) {
        this.schemaDao = schemaDao;
    }

    @Override
    public void resourceTypeAdded(ResourceType resourceType) {
        //do-nothing
    }

    @Override
    public void resourceTypeDelete(String name) {
        logger.info("Deleting schema with originalUrl: {}", name);
        schemaDao.deleteSchema(schemaDao.getSchemaByUrl(name));
    }
}
