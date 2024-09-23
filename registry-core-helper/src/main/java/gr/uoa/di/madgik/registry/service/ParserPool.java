package gr.uoa.di.madgik.registry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by stefanos on 26/6/2017.
 */
@Component("parserPool")
public class ParserPool implements ParserService {

    private final JAXBContext jaxbContext;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
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

