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

package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Schema;
import org.everit.json.schema.loader.SchemaClient;
import org.w3c.dom.ls.LSResourceResolver;

public interface SchemaDao extends LSResourceResolver, SchemaClient {

    Schema getSchema(String id);

    Schema getSchemaByUrl(String originalURL);

    String replaceLastSegment(String url, String replacingPath);

    void addSchema(Schema schema);

    void deleteSchema(Schema schema);

    javax.xml.validation.Schema loadXMLSchema(ResourceType url);

    org.everit.json.schema.Schema loadJSONSchema(ResourceType url);

}
