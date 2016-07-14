package eu.openminted.registry.core.solr.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;

@Service("solrOperationsService")
@Transactional
public class SolrOperationsService {
	private static Logger logger = Logger.getLogger(SolrOperationsService.class);

//	@Autowired
//	private Environment environment;
	
	
	public static String DEFAULT_HTTP_ADDRESS = "http://localhost:8983/solr";
	
	public static String DEFAULT_SOLR_DATA_PARENT_DIR = "/opt/solr/server/";
	
	public static String BASIC_CONFIG_DIR = "solr/configsets/omtd_registry/conf/";
	
	private static String DEFAULT_SCHEMA_XML = "schema.xml"; 
	 
    private static String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml"; 
    
    public static SolrClient getSolrClient() { 
        return new HttpSolrClient(DEFAULT_HTTP_ADDRESS);  
    } 

    public static SolrClient getSolrClient(String core) { 
    	String url;
    	if (StringUtils.isBlank(core)) { 
            url = DEFAULT_HTTP_ADDRESS; 
        } 
    	else{
    		url = DEFAULT_HTTP_ADDRESS + "/" + core;
    	}
        SolrClient solrClient = new HttpSolrClient(url); 
        return solrClient; 
    } 
 
    public void createCore(ResourceType resourceType) throws IOException, URISyntaxException, SolrServerException, ParserConfigurationException, SAXException, TransformerException, InterruptedException { 
        String core = resourceType.getName(); 
        String dataPath = DEFAULT_SOLR_DATA_PARENT_DIR + core + "/data"; 
        String confPath = DEFAULT_SOLR_DATA_PARENT_DIR + core + "/conf"; 
        createDirs(dataPath, confPath); 
        createSolrConfig(confPath); 
        createSolrSchema(resourceType.getIndexFields(), confPath); 
        SolrClient solrClient = getSolrClient(); 
        CoreAdminRequest.Create createCore = new CoreAdminRequest.Create(); 
        createCore.setDataDir(dataPath); 
        createCore.setInstanceDir(DEFAULT_SOLR_DATA_PARENT_DIR + core); 
        createCore.setCoreName(core); 
        createCore.setSchemaName(DEFAULT_SCHEMA_XML); 
        createCore.setConfigName(DEFAULT_SOLRCONFIG_XML); 
        solrClient.request(createCore); 
    } 
 
    public void createDirs(String dataPath, String confPath) { 
        File dataDir = new File(dataPath); 
        File dir = new File(DEFAULT_SOLR_DATA_PARENT_DIR); 
        File confDir = new File(confPath);
        dir.mkdirs(); 
        dataDir.mkdirs(); 
        confDir.mkdirs(); 
    } 
 
    public void createSolrConfig(String confPath) throws URISyntaxException, IOException { 
        FileUtils.copyDirectory(new File(DEFAULT_SOLR_DATA_PARENT_DIR + BASIC_CONFIG_DIR), new File(confPath)); 
    } 
 
    public void createSolrSchema(List<IndexField> indexFields, String confpath) throws ParserConfigurationException, URISyntaxException, IOException, SAXException, TransformerException { 
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance(); 
        domFactory.setIgnoringComments(true); 
        DocumentBuilder builder = domFactory.newDocumentBuilder(); 
        Document doc = builder.parse(new File(DEFAULT_SOLR_DATA_PARENT_DIR + BASIC_CONFIG_DIR + "managed-schema")); 
        NodeList nodes = doc.getElementsByTagName("schema"); 
        if(indexFields!=null){
	        for (IndexField indexField: indexFields) { 
	            Element field = doc.createElement("field"); 
	            field.setAttribute("name", indexField.getName()); 
	            field.setAttribute("type", indexField.getType()); 
	            field.setAttribute("indexed", "true"); 
	            field.setAttribute("stored", "true"); 
	            nodes.item(0).appendChild(field); 
	        } 
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance(); 
        Transformer transformer = transformerFactory.newTransformer(); 
        DOMSource source = new DOMSource(doc); 
        StreamResult streamResult =  new StreamResult(new File(confpath + "/" + DEFAULT_SCHEMA_XML)); 
        transformer.transform(source, streamResult); 
    } 

 
    public List<String> getCoreList() throws IOException, SolrServerException { 
        SolrClient solrClient = getSolrClient(); 
        CoreAdminRequest coreAdminRequest = new CoreAdminRequest(); 
        coreAdminRequest.setAction(CoreAdminParams.CoreAdminAction.STATUS); 
        CoreAdminResponse cores = coreAdminRequest.process(solrClient); 
 
        List<String> coreList = new ArrayList<String>(); 
        for (int i = 0; i < cores.getCoreStatus().size(); i++) { 
            coreList.add(cores.getCoreStatus().getName(i)); 
        } 
        return coreList; 
    }
    
    public void add(Resource resource){
		String resourceType = resource.getResourceType();
		SolrClient solrClient = SolrOperationsService.getSolrClient(resourceType);

		SolrInputDocument document = new SolrInputDocument();
		document.addField("id", resource.getId());
		document.addField("resourceType", resource.getResourceType());
		document.addField("payload", resource.getPayload());
		document.addField("version", resource.getVersion());
		document.addField("creation_date", resource.getCreationDate());
		document.addField("modification_date", resource.getModificationDate());
		if(resource.getIndexedFields()!=null){
			for (IndexedField indexedField : resource.getIndexedFields()){
				document.addField(indexedField.getName(), indexedField.getValues());
			}
		}
		
		try { 
            UpdateResponse response = solrClient.add(document);
            solrClient.commit();
            logger.debug("UpdateResponse from add of SolrInputDocument:  " + response);
        } catch (SolrServerException e) { 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "SolrServerException while adding Solr index for resource type " + resourceType, e); 
        } catch (IOException e) { 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "IOException while adding Solr index for resource type " + resourceType, e); 
        } catch (RuntimeException e) { 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "RuntimeException while adding Solr index for resource type " + resourceType, e); 
        } 
	}
	
	public void update(Resource resource){
		add(resource);
	}
	
	public void delete(Resource resource) throws PersistenceException { 
		String resourceType = resource.getResourceType();
		SolrClient solrClient = SolrOperationsService.getSolrClient(resourceType);
        if (solrClient == null) { 
            return; 
        }  
 
        try { 
        	logger.info("Deleting item with ID" + resource.getId()); 
        	solrClient.deleteById(resource.getId()); 
        } catch (SolrServerException e) { 
        	logger.info( 
                    "SolrServerException while trying to delete resource by ID for resource type " + resourceType, e); 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "SolrServerException while trying to delete resource by ID for resource type " 
                            + resourceType, e); 
        } catch (IOException e) { 
        	logger.info("IOException while trying to delete resource by ID for resource type " + resourceType, e); 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "IOException while trying to delete items by ID for persistent type " 
                            + resourceType, e); 
        } catch (RuntimeException e) { 
        	logger.info( 
                    "RuntimeException while trying to delete resource by ID for resource type " + resourceType, e); 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "RuntimeException while trying to delete resource by ID for resource type " 
                            + resourceType, e); 
        }	
	}
	
	private void doRollback(SolrClient solrClient, String type) { 
        logger.debug("ENTERING: doRollback()"); 
        try { 
        	solrClient.rollback(); 
        } catch (SolrServerException e) { 
        	logger.info("SolrServerException while doing rollback for resource type " + type, e); 
        } catch (IOException e) { 
        	logger.info("IOException while doing rollback for resource type " + type, e); 
        } 
        logger.debug("EXITING: doRollback()"); 
    } 
    
}
