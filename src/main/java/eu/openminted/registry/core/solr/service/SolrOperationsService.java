package eu.openminted.registry.core.solr.service;

import org.apache.commons.io.FileUtils; 
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient; 
import org.apache.solr.client.solrj.SolrServerException;  
import org.apache.solr.client.solrj.impl.HttpSolrClient; 
import org.apache.solr.client.solrj.request.CoreAdminRequest; 
import org.apache.solr.client.solrj.response.CoreAdminResponse; 
import org.apache.solr.common.params.CoreAdminParams; 
import org.w3c.dom.Document; 
import org.w3c.dom.Element; 
import org.w3c.dom.NodeList; 
import org.xml.sax.SAXException; 

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException; 
import javax.xml.transform.Transformer; 
import javax.xml.transform.TransformerException; 
import javax.xml.transform.TransformerFactory; 
import javax.xml.transform.dom.DOMSource; 
import javax.xml.transform.stream.StreamResult; 

import java.io.File; 
import java.io.IOException; 
import java.net.URISyntaxException;
import java.util.ArrayList; 
import java.util.List;

public class SolrOperationsService {
	private static Logger logger = Logger.getLogger(SolrOperationsService.class);

	public static final String DEFAULT_HTTP_ADDRESS = "http://localhost:8983/solr";
	
	public static final String DEFAULT_SOLR_DATA_PARENT_DIR = "/opt/solr/server/";
	
	public static final String BASIC_CONFIG_DIR = "solr/configsets/omtd_registry/conf/";
	
	private static final String DEFAULT_SCHEMA_XML = "schema.xml"; 
	 
    private static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml"; 
    
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
        for (IndexField indexField: indexFields) { 
            Element field = doc.createElement("field"); 
            field.setAttribute("name", indexField.getName()); 
            field.setAttribute("type", indexField.getType()); 
            field.setAttribute("indexed", "true"); 
            field.setAttribute("stored", "true"); 
            nodes.item(0).appendChild(field); 
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
}
