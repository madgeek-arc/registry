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

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.Resource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by stefanos on 26/6/2017.
 */
@Component
public class ParserPool implements ParserService {

    private final JAXBContext jaxbContext;
    private final ObjectMapper mapper = new ObjectMapper();

    public ParserPool(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Resource resource, Class<T> returnType) {
        T type;
        if (resource == null) {
            throw new ServiceException("null resource");
        }
        try {
            switch (resource.getPayloadFormat()) {
                case "xml":
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    type = (T) unmarshaller.unmarshal(new StringReader(resource.getPayload()));
                    break;
                case "json":
                    type = mapper.readValue(resource.getPayload(), returnType);
                    break;
                default:
                    throw new ServiceException("Unsupported media type");
            }
        } catch (Exception je) {
            throw new ServiceException(je);
        }
        return type;
    }

    public String serialize(Object resource, ParserServiceTypes mediaType) {
        try {
            if (mediaType == ParserServiceTypes.XML) {
                Marshaller marshaller = jaxbContext.createMarshaller();
                StringWriter sw = new StringWriter();
                marshaller.marshal(resource, sw);
                return sw.toString();
            } else if (mediaType == ParserServiceTypes.JSON) {
                return mapper.writeValueAsString(resource);
            } else {
                throw new ServiceException("Unsupported media type");
            }
        } catch (Exception e) {
            throw new ServiceException(e);
        }

    }
}

