package eu.openminted.registry.core.index;

import eu.openminted.registry.core.service.ServiceException;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by antleb on 5/21/16.
 */
@Component
public class XMLFieldParser implements FieldParser {

    private static final Logger logger = LoggerFactory.getLogger(XMLFieldParser.class);

    public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

        Set<Object> objects;

        try {
            Processor processor = new Processor(false);
            XdmNode xdm = processor.newDocumentBuilder().build(new StreamSource(new StringReader(payload)));

            if (isMultiValued) {
                XdmValue result = processor.newXPathCompiler().evaluate(path, xdm);

                logger.debug("found " + result.size() + " values for" + path);
                objects = result
                        .stream()
                        .map(XdmValue::toString)
                        .map(answer -> FieldParser.parseField(fieldType, answer))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
            } else {
                String response;
                response = processor.newXPathCompiler().evaluate(path, xdm).toString();
                objects = FieldParser.parseField(fieldType, response);
            }
        } catch (Exception e) {
            throw new ServiceException("Error in parsing XML document [" + e.getMessage() + "]", e);
        }

        return objects;
    }
}
