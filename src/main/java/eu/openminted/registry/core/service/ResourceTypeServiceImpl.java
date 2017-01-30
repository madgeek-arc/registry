package eu.openminted.registry.core.service;

import eu.openminted.registry.core.dao.ResourceTypeDao;
import eu.openminted.registry.core.dao.SchemaDao;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.Schema;
import eu.openminted.registry.core.domain.Tools;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.index.DefaultIndexMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlunit.builder.Input;
import org.xmlunit.validation.Languages;
import org.xmlunit.validation.ValidationResult;
import org.xmlunit.validation.Validator;

import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by antleb on 7/14/16.
 */
@Service("resourceTypeService")
@Transactional
public class ResourceTypeServiceImpl implements ResourceTypeService {

	private static Logger logger = Logger.getLogger(ResourceTypeService.class);

	@Autowired
	ResourceTypeDao resourceTypeDao;

	@Autowired
	SchemaDao schemaDao;

	public ResourceTypeServiceImpl() {

	}

	@Override
	public Schema getSchema(String id) {
		Schema schema = schemaDao.getSchema(id);
		return schema;
	}

	@Override
	public ResourceType getResourceType(String name) {
		ResourceType resourceType = resourceTypeDao.getResourceType(name);
		return resourceType;
	}

	@Override
	public List<ResourceType> getAllResourceType() {
		List<ResourceType> resourceType = resourceTypeDao.getAllResourceType();

		return resourceType;
	}

	@Override
	public List<ResourceType> getAllResourceType(int from, int to) {
		List<ResourceType> resourceType = resourceTypeDao.getAllResourceType(from, to);
		return resourceType;
	}

	@Override
	public ResourceType addResourceType(ResourceType resourceType) throws ServiceException {
		Schema schema = new Schema();


		if (resourceTypeDao.getResourceType(resourceType.getName()) != null) {
			throw new ServiceException("{\"error\":\"Schema already created\"}");
		}

		if (resourceType.getSchemaUrl() == null && resourceType.getSchema() == null) {
			throw new ServiceException("{\"error\":\"Neither SchemaUrl nor Schema have been set.\"}");
		} else if (resourceType.getSchemaUrl() != null && resourceType.getSchema() != null) {
			throw new ServiceException("{\"error\":\"Both Schema and SchemaUrl are set\"}");
		} else {
			if (resourceType.getSchemaUrl() == null) {
				resourceType.setSchemaUrl("not_set");
			} else {
				try {
					String output = "";
					output = Tools.getText(resourceType.getSchemaUrl());
					resourceType.setSchema(output);
				} catch (Exception e) {
					throw new ServiceException("{\"error\":\""+e.getMessage()+"\"}");
				}
			}
		}
		
		
		if (resourceType.getIndexMapperClass() == null)
			resourceType.setIndexMapperClass(DefaultIndexMapper.class.getName());

		if (resourceType.getIndexFields() != null) {
			for (IndexField field : resourceType.getIndexFields())
				field.setResourceType(resourceType);
		}

		ArrayList<String> recursionPaths = new ArrayList<String>();
		
		exportIncludes(resourceType, resourceType.getSchemaUrl(),recursionPaths);

		try {
			resourceTypeDao.addResourceType(resourceType);
		} catch (HibernateException e) {
			throw new ServiceException(e);
		}

		schema.setId(stringToMd5(resourceType.getSchema()));

		if (resourceType.getSchemaUrl() != null) {
			schema.setOriginalUrl(resourceType.getSchemaUrl());
		}

		schema.setSchema(resourceType.getSchema());

		schemaDao.addSchema(schema);

		return resourceType;
	}

	public ResourceTypeDao getResourceTypeDao() {
		return resourceTypeDao;
	}

	public void setResourceTypeDao(ResourceTypeDao resourceTypeDao) {
		this.resourceTypeDao = resourceTypeDao;
	}

	private String stringToMd5(String stringToBeConverted) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(stringToBeConverted.toString().getBytes());
			byte[] digest = md.digest();
			StringBuffer sb = new StringBuffer();
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

//	private void exportIncludes(ResourceType resourceType, String baseUrl, ArrayList<String> recursionPaths) throws ServiceException {
//		String type = resourceType.getPayloadType();
//		boolean isFromUrl;
//
//		if (resourceType.getSchemaUrl().equals("not_set")) {
//			isFromUrl = false;
//		} else {
//			isFromUrl = true;
//		}
//
//		if (type.equals("xml")) {
//			try {
//				validateScema(resourceType.getSchema());
//				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//				dbFactory.setNamespaceAware(true);
////				dbFactory.setValidating(true);
//				DocumentBuilder dBuilder;
//
//				dBuilder = dbFactory.newDocumentBuilder();
//
//				Document doc = dBuilder.parse(new InputSource(new StringReader(resourceType.getSchema())));
//				doc.getDocumentElement().normalize();
//
//
//				XPathFactory factory = XPathFactory.newInstance();
//				XPath xpath = factory.newXPath();
//				final String prefixFinal = "";
//
//				// there's no default implementation for NamespaceContext...seems kind of silly, no?
//				xpath.setNamespaceContext(new NamespaceContext() {
//					public String getNamespaceURI(String prefix) {
//						if (prefix == null) return "http://www.w3.org/2001/XMLSchema";
//						else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
//						else if ("xs".equals(prefix)) return "http://www.w3.org/2001/XMLSchema";
//						else if ("xsd".equals(prefix)) return "http://www.w3.org/2001/XMLSchema";
//						return XMLConstants.NULL_NS_URI;
//					}
//
//					// This method isn't necessary for XPath processing.
//					public String getPrefix(String uri) {
//						throw new UnsupportedOperationException();
//					}
//
//					// This method isn't necessary for XPath processing either.
//					public Iterator getPrefixes(String uri) {
//						throw new UnsupportedOperationException();
//					}
//				});
//				String expression = "//xs:include/attribute::schemaLocation";
//				NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
//				for (int i = 0; i < nodeList.getLength(); i++) {
//					String schemaUrl = nodeList.item(i).getTextContent();
//
//					logger.debug("Checking schema: " + schemaUrl);
//
//					int validation = isValidUrl(schemaUrl, isFromUrl);
//					if (validation != 0) {
//						String schemaContent;
//						
//						Schema schema = schemaDao.getSchemaByUrl(schemaUrl);
//						if(schema==null){
//							schema = new Schema();
//							schema.setOriginalUrl(schemaUrl);
//							String originalUrl = schemaUrl;
//							if (validation == 2) {
//								schemaUrl = baseUrl.replace(baseUrl.substring(baseUrl.lastIndexOf("/") + 1), schemaUrl);
//							}
//							try {
//								schemaContent = Tools.getText(schemaUrl);
//							} catch (Exception e) {
//								throw new ServiceException("failed to download file(s)", e);
//							}
//							
//							resourceType.setSchema(schemaContent);
//							
//							if(recursionPaths.contains(originalUrl)){
//								nodeList.item(i).setNodeValue(getBaseEnvLinkURL() + "/schemaService/" + schema.getId() + "");
//							}else{
//								schema.setId(stringToMd5(resourceType.getSchema()));
//								recursionPaths.add(schemaUrl);
//								exportIncludes(resourceType,baseUrl,recursionPaths);
//								
//								schema.setSchema(resourceType.getSchema());
//								nodeList.item(i).setNodeValue(getBaseEnvLinkURL() + "/schemaService/" + schema.getId() + "");
//								schemaDao.addSchema(schema);
//							}
//							
//						}else{
//							nodeList.item(i).setNodeValue(getBaseEnvLinkURL() + "/schemaService/" + schema.getId());
//						}
//						
//					} else {
//						throw new ServiceException("includes contain relative paths that cannot be resolved");
//					}
//				}
//				resourceType.setSchema(documentToString(doc));
//			} catch (ServiceException e) {
//				throw e;
//			} catch (Exception e) {
//				throw new ServiceException(e);
//			}
//		}
//	}
	
	private void exportIncludes(ResourceType resourceType, String baseUrl, ArrayList<String> recursionPaths) throws ServiceException {
		String type = resourceType.getPayloadType();
		boolean isFromUrl;

		if (resourceType.getSchemaUrl().equals("not_set")) {
			isFromUrl = false;
		} else {
			isFromUrl = true;
		}

		if (type.equals("xml")) {
			try {
				validateScema(resourceType.getSchema());
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				dbFactory.setNamespaceAware(true);
//				dbFactory.setValidating(true);
				DocumentBuilder dBuilder;

				dBuilder = dbFactory.newDocumentBuilder();

				Document doc = dBuilder.parse(new InputSource(new StringReader(resourceType.getSchema())));
				doc.getDocumentElement().normalize();


				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				final String prefixFinal = "";

				// there's no default implementation for NamespaceContext...seems kind of silly, no?
				xpath.setNamespaceContext(new NamespaceContext() {
					public String getNamespaceURI(String prefix) {
						if (prefix == null) return "http://www.w3.org/2001/XMLSchema";
						else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
						else if ("xs".equals(prefix)) return "http://www.w3.org/2001/XMLSchema";
						else if ("xsd".equals(prefix)) return "http://www.w3.org/2001/XMLSchema";
						return XMLConstants.NULL_NS_URI;
					}

					// This method isn't necessary for XPath processing.
					public String getPrefix(String uri) {
						throw new UnsupportedOperationException();
					}

					// This method isn't necessary for XPath processing either.
					public Iterator getPrefixes(String uri) {
						throw new UnsupportedOperationException();
					}
				});
				String expression = "//xs:include/attribute::schemaLocation";
				NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);
				for (int i = 0; i < nodeList.getLength(); i++) {
					String schemaUrl = nodeList.item(i).getTextContent();

					logger.debug("Checking schema: " + schemaUrl);

					int validation = isValidUrl(schemaUrl, isFromUrl);
					if (validation != 0) {
						String schemaContent;

						if (validation == 2) {
							schemaUrl = baseUrl.replace(baseUrl.substring(baseUrl.lastIndexOf("/") + 1), schemaUrl);
						}

						try {
							schemaContent = Tools.getText(schemaUrl);
						} catch (Exception e) {
							throw new ServiceException("failed to download file(s)", e);
						}

						Schema schema = schemaDao.getSchema(stringToMd5(schemaContent));

						if (schema != null) {
							logger.debug("Schema " + schemaUrl + " is already in the db. Ignoring...");
							nodeList.item(i).setNodeValue(getBaseEnvLinkURL() + "/schemaService/" + schema.getId());
						} else {
							//add schema in db and call the "exportIncludes" function again
							
							if(recursionPaths.contains(stringToMd5(schemaContent))){
								nodeList.item(i).setNodeValue(getBaseEnvLinkURL() + "/schemaService/" + stringToMd5(schemaContent));
							}else{
								schema = new Schema();
								schema.setId(stringToMd5(schemaContent));
								resourceType.setSchema(schemaContent);
								recursionPaths.add(stringToMd5(schemaContent));
								exportIncludes(resourceType, schemaUrl,recursionPaths);
								
								schema.setSchema(resourceType.getSchema());
								schema.setOriginalUrl(nodeList.item(i).getNodeValue());
								nodeList.item(i).setNodeValue(getBaseEnvLinkURL() + "/schemaService/" + schema.getId() + "");

								try {
									schemaDao.addSchema(schema);
								} catch (NonUniqueObjectException e) {
									throw new ServiceException(e);
								}
							}
							
							
						}
					} else {
						throw new ServiceException("includes contain relative paths that cannot be resolved");
					}
				}
				resourceType.setSchema(documentToString(doc));
			} catch (ServiceException e) {
				throw e;
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		}
	}

	

	private void validateScema(String schema) throws ServiceException {
		Validator validator = Validator.forLanguage(Languages.W3C_XML_SCHEMA_NS_URI);

		logger.debug("Validating schema");

		validator.setSchemaSource(Input.fromURI("https://www.w3.org/2001/XMLSchema.xsd").build());
		ValidationResult result = validator.validateInstance(new StreamSource( new StringReader(schema)));

		if (!result.isValid()) {
			throw new ServiceException("Invalid xsd: " + result.getProblems());
		}

		logger.debug("Schema is valid");
	}

	private static int isValidUrl(String Url, boolean isFromUrl) {
		URI u;
		try {
			u = new URI(Url);
		} catch (URISyntaxException e) {
			return 0;
		}

		if (u.isAbsolute()) {
			return 1;
		} else {
			if (isFromUrl) {
				return 2;
			} else {
				return 0;
			}
		}
	}

	protected static String getBaseEnvLinkURL() {

		String baseEnvLinkURL = null;
		HttpServletRequest currentRequest =
				((ServletRequestAttributes) RequestContextHolder.
						currentRequestAttributes()).getRequest();
		// lazy about determining protocol but can be done too
		baseEnvLinkURL = "http://" + currentRequest.getServerName();
		if (currentRequest.getLocalPort() != 80) {
			baseEnvLinkURL += ":" + currentRequest.getLocalPort();
		}
		if (!StringUtils.isEmpty(currentRequest.getContextPath())) {
			baseEnvLinkURL += currentRequest.getContextPath();
		}
		return baseEnvLinkURL;
	}

	private String documentToString(Document document) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new DOMSource(document), new StreamResult(sw));
			return sw.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}
}