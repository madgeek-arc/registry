package eu.openminted.registry.core.index;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Date;
import java.util.HashSet;
import java.util.Set;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Created by antleb on 5/21/16.
 */
public class XMLFieldParser implements FieldParser {

	private static Logger logger = Logger.getLogger(XMLFieldParser.class);

	public Set<Object> parse(String payload, String fieldType, String path, boolean isMultiValued) {

		Set<Object> objects = new HashSet<Object>();
		
		try {
	         DocumentBuilderFactory dbFactory 
	            = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder;

	         dBuilder = dbFactory.newDocumentBuilder();

	         Document doc = dBuilder.parse(new InputSource(new StringReader(payload)));
	         doc.getDocumentElement().normalize();

	         XPath xPath =  XPathFactory.newInstance().newXPath();

	         String expression = path;        
	        if(isMultiValued){
		         NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

				logger.debug("found " + nodeList.getLength() + " values for" + expression);

		         for (int i = 0; i < nodeList.getLength(); i++) {
		            Node nNode = nodeList.item(i);
		            String response = "";
		            response = nNode.getTextContent();
		            FieldParser.parseField(fieldType,response,objects);
		         }
		      
	        }else{
	        	String response = "";
	        	response = (String) xPath.compile(expression).evaluate(doc,	XPathConstants.STRING);
				FieldParser.parseField(fieldType,response,objects);
	        }
	      } catch (ParserConfigurationException e) {
	    	  objects.add(e.getMessage());
	      } catch (SAXException e) {
	    	  objects.add(e.getMessage());
	      } catch (IOException e) {
	    	  objects.add(e.getMessage());
	      } catch (XPathExpressionException e) {
	         objects.add(e.getMessage());
	      }
		
		return objects;
	}
}
