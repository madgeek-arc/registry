package eu.openminted.registry.core.index;

import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by antleb on 5/21/16.
 */
@Component
public class XMLFieldParser implements FieldParser {

    private static Logger logger = LogManager.getLogger(XMLFieldParser.class);

    public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

        Set<Object> objects;

        try {
            DocumentBuilderFactory dbFactory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;

            dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(new InputSource(new StringReader(payload)));
            doc.getDocumentElement().normalize();

            XPath xPath = XPathFactory.newInstance().newXPath();

            if (isMultiValued) {
                NodeList nodeList = (NodeList) xPath.compile(path).evaluate(doc, XPathConstants.NODESET);

                logger.debug("found " + nodeList.getLength() + " values for" + path);
                objects = IntStream.range(0,nodeList.getLength())
                        .mapToObj(nodeList::item)
                        .map(Node::getTextContent)
                        .map(answer -> FieldParser.parseField(fieldType, answer))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
            } else {
                String response;
                response = (String) xPath.compile(path).evaluate(doc, XPathConstants.STRING);
                objects = FieldParser.parseField(fieldType, response);
            }
        } catch (Exception e) {
            throw new ServiceException("Error in parsing XML document [" + e.getMessage() + "]",e);
        }

        return objects;
    }
}
