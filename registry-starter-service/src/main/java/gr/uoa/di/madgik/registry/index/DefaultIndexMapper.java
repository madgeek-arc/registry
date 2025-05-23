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

package gr.uoa.di.madgik.registry.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultIndexMapper implements IndexMapper {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIndexMapper.class);
    private final IndexedFieldFactory indexedFieldFactory;
    private final XMLFieldParser xmlFieldParser;
    private final JSONFieldParser jsonFieldParser;
    private List<IndexField> indexFields;

    public DefaultIndexMapper(IndexedFieldFactory indexedFieldFactory, XMLFieldParser xmlFieldParser,
                              JSONFieldParser jsonFieldParser) {
        this.indexedFieldFactory = indexedFieldFactory;
        this.xmlFieldParser = xmlFieldParser;
        this.jsonFieldParser = jsonFieldParser;
    }

    public List<IndexedField> getValues(String payload, ResourceType resourceType) throws ServiceException {
        List<IndexedField> res = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (IndexField indexField : resourceType.getIndexFields()) {
            try {
                String fieldName = indexField.getName();
                String fieldType = indexField.getType();
                String path = indexField.getPath();
                String value = indexField.getDefaultValue();
                Set<Object> values;
                logger.debug("Indexing field " + fieldName + " (" + fieldType + ") with path " + path + " and DEFAULT VALUE:" + value);

                //if there is no xpath add default value
                if (path == null) {
                    values = new HashSet<>();
                    if (value == null) {
                        throw new ServiceException("Indexfield:" + fieldName + " with no xpath must supply a default value");
                    }

                    values.add(mapper.convertValue(value, Class.forName(indexField.getType())));
                    res.add(indexedFieldFactory.getIndexedField(fieldName, values, fieldType));
                } else {
                    //there is an xpath
                    values = getValue(payload, fieldType, path, resourceType.getPayloadType(), indexField.isMultivalued());
                    if (values != null && !values.isEmpty()) {
                        res.add(indexedFieldFactory.getIndexedField(fieldName, values, fieldType));
                    } else {
                        values = new HashSet<>();
                        if (value != null) {
                            values.add(mapper.convertValue(value, Class.forName(indexField.getType())));
                        }

                        res.add(indexedFieldFactory.getIndexedField(fieldName, values, fieldType));
                    }

                }
            } catch (Exception e) {
                throw new ServiceException(e.getMessage());
            }
        }

        return res;
    }

    private Set<Object> getValue(String payload, String fieldType, String path, String payloadType, boolean isMultiValued) {
        FieldParser fieldParser;

        if (payloadType.equals("json"))
            fieldParser = jsonFieldParser;
        else if (payloadType.equals("xml"))
            fieldParser = xmlFieldParser;
        else
            fieldParser = null;

        return fieldParser.parse(payload, fieldType, path, isMultiValued);
    }

    @Override
    public List<IndexField> getIndexFields() {
        return indexFields;
    }

    public void setIndexFields(List<IndexField> indexFields) {
        this.indexFields = indexFields;
    }
}
