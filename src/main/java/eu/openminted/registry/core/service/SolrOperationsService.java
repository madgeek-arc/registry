package eu.openminted.registry.core.service;

import org.apache.commons.io.FileUtils; 
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient; 
import org.apache.solr.client.solrj.SolrServerException;  
import org.apache.solr.client.solrj.impl.HttpSolrClient; 
import org.apache.solr.client.solrj.request.CoreAdminRequest; 
import org.apache.solr.client.solrj.response.CoreAdminResponse; 
import org.apache.solr.common.params.CoreAdminParams; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList; 
import java.util.List;

public class SolrOperationsService {
	private static Logger logger = Logger.getLogger(SolrOperationsService.class);
	
	@Autowired
	private Environment env;
 
    private String dataDir; 
    
    @Bean
    public SolrClient solrClient() throws MalformedURLException, IllegalStateException {
    	// TODO: add env.getRequiredProperty("solr.host")
    	return new HttpSolrClient("http://localhost:8983/solr/");
    }
 
    public SolrOperationsService(String dataDir) {  
    	// TODO: add env.getRequiredProperty("solr.dir")
    	this.dataDir = dataDir;
    } 
 
    public void createCore(ResourceType resourceType) throws IOException, URISyntaxException, SolrServerException, ParserConfigurationException, SAXException, TransformerException, InterruptedException { 
        String core = resourceType.getName(); 
        String dataPath = this.dataDir + '/' + core + "/data"; 
        String confPath = this.dataDir + '/' + core + "/conf"; 
        createDirs(dataPath, confPath); 
        createSolrConfig(confPath); 
        createSolrSchema(resourceType.getIndexFields(), confPath); 
        SolrClient solrClient = solrClient(); 
        CoreAdminRequest.Create createCore = new CoreAdminRequest.Create(); 
        createCore.setDataDir(dataPath); 
        createCore.setInstanceDir(dataDir + '/' + core); 
        createCore.setCoreName(core); 
        createCore.setSchemaName("schema.xml"); 
        createCore.setConfigName("solrconfig.xml"); 
        solrClient.request(createCore); 
    } 
 
    public void createDirs(String dataPath, String confPath) { 
        File dataDir = new File(dataPath); 
        File dir = new File(this.dataDir); 
        File confDir = new File(confPath);
        dir.mkdirs(); 
        dataDir.mkdirs(); 
        confDir.mkdirs(); 
    } 
 
    public void createSolrConfig(String confPath) throws URISyntaxException, IOException { 
        FileUtils.copyDirectory(new File(this.dataDir + "/solr/configsets/omtd_registry/conf/"), new File(confPath)); 
        File managedSchema = new File(confPath + "managed-schema");
        managedSchema.delete();
    } 
 
    public void createSolrSchema(List<IndexField> indexFields, String confpath) throws ParserConfigurationException, URISyntaxException, IOException, SAXException, TransformerException { 
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance(); 
        domFactory.setIgnoringComments(true); 
        DocumentBuilder builder = domFactory.newDocumentBuilder(); 
     // TODO: Replace to ClassLoader
        Document doc = builder.parse(new File(this.dataDir + "/solr/configsets/omtd_registry/conf/managed-schema")); 
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
        StreamResult streamResult =  new StreamResult(new File(confpath+"/schema.xml")); 
        transformer.transform(source, streamResult); 
    } 

 
    public List<String> getCoreList() throws IOException, SolrServerException { 
        SolrClient solrClient = solrClient(); 
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
