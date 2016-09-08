package eu.openminted.registry.core.domain;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.dnetlib.functionality.index.cql.CqlTranslator;

public class Tools {
	
	private static Logger logger = Logger.getLogger(Tools.class);
	
	  public static String getText(String url) throws Exception {

		  String out = new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next();
		 
		  
			  if(out==null || out.isEmpty()){
				  return null;
			  }else{
				  return out;
			  }
		}
	  


	  
	  public static String validateJSONSchema(String schemaContent, String jsonContent){
		  
		  InputStream stream = new ByteArrayInputStream(schemaContent.getBytes(StandardCharsets.UTF_8));
		  JSONObject rawSchema = new JSONObject(new JSONTokener(stream));
	      org.everit.json.schema.Schema schema = SchemaLoader.load(rawSchema);
	      try{
	    	  schema.validate(new JSONObject(jsonContent)); // throws a ValidationException if this object is invalid
		  }catch(ValidationException e){
			  return e.getMessage();
		  }
		  return "true";
	  }
	    
	    
	    public static String validateXMLSchema(String xsdContent, String xmlContent){
//  	      try {
//
//  	         SchemaFactory factory =
//  	            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//  	         java.io.FileWriter fwXML = new java.io.FileWriter("testXML.xml");
//  	         fwXML.write(xmlContent);
//  	         fwXML.close();
//
//  	         java.io.FileWriter fwXSD = new java.io.FileWriter("testXSD.xsd");
//  	         fwXSD.write(xsdContent);
//  	         fwXSD.close();
//
//
//  	         Schema schema = factory.newSchema(new File("testXSD.xsd"));
//  	            Validator validator = schema.newValidator();
//  	            validator.validate(new StreamSource(new File("testXML.xml")));
//  	      } catch (IOException e){
//  	         return e.getMessage();
//  	      }catch(SAXException e1){
//  	         return e1.getMessage();
//  	      } catch (Exception e) {
//				return e.getMessage();
//			}
  	      return "true";
  	   }
	    
  }

		

